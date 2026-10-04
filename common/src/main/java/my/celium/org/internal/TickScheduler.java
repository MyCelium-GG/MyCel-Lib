package my.celium.org.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import my.celium.org.logging.MycelLog;
import my.celium.org.schedule.ScheduledTask;

/**
 * Internal server-thread task queue, pumped once per server tick by the
 * loader modules.
 *
 * <p>Not public API; use {@code MycelScheduler}.
 */
public final class TickScheduler {
    private record Once(long tick, Runnable task) {
    }

    private static final class Repeating implements ScheduledTask {
        final long interval;
        final Runnable task;
        long nextTick;
        volatile boolean cancelled;

        Repeating(long startTick, long interval, Runnable task) {
            this.nextTick = startTick;
            this.interval = interval;
            this.task = task;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }

    private static final List<Once> ONCE = new ArrayList<>();
    private static final List<Repeating> REPEATING = new ArrayList<>();
    private static long tick;

    private TickScheduler() {
    }

    public static synchronized void runLater(long delayTicks, Runnable task) {
        ONCE.add(new Once(tick + Math.max(0, delayTicks), task));
    }

    public static synchronized ScheduledTask runEvery(long intervalTicks, Runnable task) {
        if (intervalTicks < 1) {
            throw new IllegalArgumentException("intervalTicks must be >= 1");
        }
        Repeating repeating = new Repeating(tick + intervalTicks, intervalTicks, task);
        REPEATING.add(repeating);
        return repeating;
    }

    /** Advances one tick, running everything due. Runs on the server thread. */
    public static void tick() {
        List<Runnable> due = new ArrayList<>();
        synchronized (TickScheduler.class) {
            tick++;
            Iterator<Once> once = ONCE.iterator();
            while (once.hasNext()) {
                Once entry = once.next();
                if (entry.tick() <= tick) {
                    due.add(entry.task());
                    once.remove();
                }
            }
            Iterator<Repeating> repeating = REPEATING.iterator();
            while (repeating.hasNext()) {
                Repeating entry = repeating.next();
                if (entry.cancelled) {
                    repeating.remove();
                } else if (entry.nextTick <= tick) {
                    due.add(entry.task);
                    entry.nextTick = tick + entry.interval;
                }
            }
        }
        for (Runnable task : due) {
            try {
                task.run();
            } catch (RuntimeException e) {
                MycelLog.warn("Scheduled server task threw; skipping: {}", e.toString());
            }
        }
    }

    /** Drops all queued server tasks (called on server stop). */
    public static synchronized void clear() {
        ONCE.clear();
        for (Repeating repeating : REPEATING) {
            repeating.cancel();
        }
        REPEATING.clear();
    }

    static synchronized long currentTick() {
        return tick;
    }
}

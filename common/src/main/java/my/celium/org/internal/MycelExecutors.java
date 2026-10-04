package my.celium.org.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Internal shared executor for safe background work (update checks and other
 * non-game-thread IO).
 *
 * <p>Not public API. Exactly one cached pool exists no matter how many mods
 * use Mycel; threads are daemons and die after 60s idle. Background work must
 * never touch world state - schedule the game-thread part with
 * {@code MycelScheduler} instead.
 */
public final class MycelExecutors {
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final ThreadFactory FACTORY = runnable -> {
        Thread thread = new Thread(runnable, "mycel-async-" + COUNTER.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    };

    private static volatile ExecutorService pool;

    private MycelExecutors() {
    }

    public static ExecutorService async() {
        ExecutorService current = pool;
        if (current == null || current.isShutdown()) {
            synchronized (MycelExecutors.class) {
                if (pool == null || pool.isShutdown()) {
                    pool = Executors.newCachedThreadPool(FACTORY);
                }
                current = pool;
            }
        }
        return current;
    }

    /** Stops background work; called on server stop. The pool is recreated lazily. */
    public static synchronized void shutdown() {
        if (pool != null) {
            pool.shutdown();
            try {
                pool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                pool.shutdownNow();
                pool = null;
            }
        }
    }
}

package my.celium.org.schedule;

import java.util.Objects;

import my.celium.org.internal.MycelExecutors;
import my.celium.org.internal.ServerHolder;
import my.celium.org.internal.TickScheduler;
import net.minecraft.server.MinecraftServer;

/**
 * Safe scheduling helpers.
 *
 * <ul>
 *   <li>{@link #runLater} / {@link #runEvery} execute on the <b>server thread</b>
 *       and may touch worlds, entities and players.</li>
 *   <li>{@link #runAsync} executes on a shared background pool for IO such as
 *       HTTP. It must <b>never</b> touch game state; hop back with
 *       {@link #runOnServer} when done.</li>
 * </ul>
 */
public final class MycelScheduler {
    private MycelScheduler() {
    }

    /**
     * Runs {@code task} on the server thread after {@code delayTicks} server ticks.
     * When no server is running yet, the task waits for the next server tick.
     */
    public static void runLater(long delayTicks, Runnable task) {
        TickScheduler.runLater(delayTicks, Objects.requireNonNull(task, "task"));
    }

    /**
     * Runs {@code task} on the server thread every {@code intervalTicks} ticks.
     * The returned handle cancels future runs (also cancelled automatically on server stop).
     */
    public static ScheduledTask runEvery(long intervalTicks, Runnable task) {
        return TickScheduler.runEvery(intervalTicks, Objects.requireNonNull(task, "task"));
    }

    /** Runs {@code task} on Mycel's shared background pool. Never touch game state here. */
    public static void runAsync(Runnable task) {
        MycelExecutors.async().execute(Objects.requireNonNull(task, "task"));
    }

    /**
     * Queues {@code task} onto the server thread. Safe to call from background
     * threads; if no server is running the task is dropped with a warning.
     */
    public static void runOnServer(Runnable task) {
        Objects.requireNonNull(task, "task");
        MinecraftServer server = ServerHolder.get();
        if (server == null) {
            my.celium.org.logging.MycelLog.warn("runOnServer dropped: no server running");
            return;
        }
        server.execute(task);
    }

    /** Is the current thread the server thread? Useful for assertions in mod code. */
    public static boolean isServerThread() {
        MinecraftServer server = ServerHolder.get();
        return server != null && Thread.currentThread() == server.getRunningThread();
    }
}

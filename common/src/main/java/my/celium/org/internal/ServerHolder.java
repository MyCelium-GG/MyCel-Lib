package my.celium.org.internal;

import net.minecraft.server.MinecraftServer;

/**
 * Internal holder for the running {@link MinecraftServer}.
 *
 * <p>Set by each loader module on server start, cleared on server stop.
 * Scheduling helpers and server-side utilities resolve the server through
 * here instead of keeping their own static references.
 */
public final class ServerHolder {
    private static volatile MinecraftServer server;

    private ServerHolder() {
    }

    public static void set(MinecraftServer current) {
        server = current;
    }

    public static void clear() {
        server = null;
    }

    /** Returns the running server, or {@code null} when no server is active. */
    public static MinecraftServer get() {
        return server;
    }

    /** Returns the running server or throws with an actionable message. */
    public static MinecraftServer require() {
        MinecraftServer current = server;
        if (current == null) {
            throw new IllegalStateException("No Minecraft server is running (Mycel ServerHolder is empty)");
        }
        return current;
    }

    public static boolean isRunning() {
        return server != null;
    }
}

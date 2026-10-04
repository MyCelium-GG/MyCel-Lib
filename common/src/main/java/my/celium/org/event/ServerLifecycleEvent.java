package my.celium.org.event;

import net.minecraft.server.MinecraftServer;

/** Fired when the dedicated/integrated server starts or stops. */
public final class ServerLifecycleEvent {
    /** Lifecycle phase. */
    public enum Phase {
        STARTED,
        STOPPED
    }

    private final MinecraftServer server;
    private final Phase phase;

    public ServerLifecycleEvent(MinecraftServer server, Phase phase) {
        this.server = server;
        this.phase = phase;
    }

    public MinecraftServer server() {
        return server;
    }

    public Phase phase() {
        return phase;
    }
}

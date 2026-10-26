package my.celium.org.event;

import net.minecraft.server.MinecraftServer;

/** Fired when the dedicated/integrated server starts or stops. */
public final class ServerLifecycleEvent {
    /** Lifecycle phase. */
    public enum Phase {
        /** The server has started and is about to tick. */
        STARTED,
        /** The server has stopped. */
        STOPPED
    }

    private final MinecraftServer server;
    private final Phase phase;

    /**
     * Created by the loader modules; mods only receive it.
     *
     * @param server the server
     * @param phase  what happened
     */
    public ServerLifecycleEvent(MinecraftServer server, Phase phase) {
        this.server = server;
        this.phase = phase;
    }

    /**
     * The server that started or stopped.
     *
     * @return the server
     */
    public MinecraftServer server() {
        return server;
    }

    /**
     * What happened.
     *
     * @return the phase
     */
    public Phase phase() {
        return phase;
    }
}

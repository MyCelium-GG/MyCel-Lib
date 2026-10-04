package my.celium.org.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Context for a received packet. Handlers always run on the game thread —
 * the loader wiring schedules them there — so handlers may touch game state.
 */
public final class PacketContext {
    private final PacketDirection direction;
    private final MinecraftServer server;
    private final ServerPlayer sender;

    public PacketContext(PacketDirection direction, MinecraftServer server, ServerPlayer sender) {
        this.direction = direction;
        this.server = server;
        this.sender = sender;
    }

    public PacketDirection direction() {
        return direction;
    }

    /** The server, or {@code null} on the client. */
    public MinecraftServer server() {
        return server;
    }

    /**
     * The player who sent a {@link PacketDirection#TO_SERVER} packet,
     * or {@code null} for {@link PacketDirection#TO_CLIENT} packets.
     */
    public ServerPlayer sender() {
        return sender;
    }
}

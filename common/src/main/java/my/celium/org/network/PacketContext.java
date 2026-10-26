package my.celium.org.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Context for a received packet. Handlers always run on the game thread:
 * the loader wiring schedules them there - so handlers may touch game state.
 */
public final class PacketContext {
    private final PacketDirection direction;
    private final MinecraftServer server;
    private final ServerPlayer sender;

    /**
     * Created by the loader wiring; handlers only receive it.
     *
     * @param direction which way the packet travelled
     * @param server    the server, or {@code null} on the client
     * @param sender    the sending player for serverbound packets, else {@code null}
     */
    public PacketContext(PacketDirection direction, MinecraftServer server, ServerPlayer sender) {
        this.direction = direction;
        this.server = server;
        this.sender = sender;
    }

    /**
     * Which way the packet travelled.
     *
     * @return the direction
     */
    public PacketDirection direction() {
        return direction;
    }

    /**
     * The server, or {@code null} on the client.
     *
     * @return the server, or {@code null}
     */
    public MinecraftServer server() {
        return server;
    }

    /**
     * The player who sent a {@link PacketDirection#TO_SERVER} packet,
     * or {@code null} for {@link PacketDirection#TO_CLIENT} packets.
     *
     * @return the sender, or {@code null}
     */
    public ServerPlayer sender() {
        return sender;
    }
}

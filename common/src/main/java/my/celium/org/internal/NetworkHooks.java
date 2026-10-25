package my.celium.org.internal;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Internal bridge from the loader networking modules to {@code MycelNetwork}.
 *
 * <p>Not public API. Each loader installs how it knows whether a player's
 * connection (or, on the client, the server's) has a channel; until then
 * every peer counts as reachable. Packets go out as plain vanilla custom
 * payloads unless the loader needs its own wrapping (Forge does).
 */
public final class NetworkHooks {
    private static volatile BiPredicate<ServerPlayer, Identifier> presence = (player, id) -> true;
    private static volatile Predicate<Identifier> serverPresence = id -> true;
    private static volatile BiConsumer<ServerPlayer, CustomPacketPayload> toClient =
            (player, payload) -> player.connection.send(new ClientboundCustomPayloadPacket(payload));
    private static volatile Consumer<CustomPacketPayload> toServer;

    private NetworkHooks() {
    }

    public static void setPresence(BiPredicate<ServerPlayer, Identifier> check) {
        presence = Objects.requireNonNull(check, "check");
    }

    /** Client side: whether the connected server has a channel. Only called on the client. */
    public static void setServerPresence(Predicate<Identifier> check) {
        serverPresence = Objects.requireNonNull(check, "check");
    }

    public static boolean isPresentOnServer(Identifier channel) {
        try {
            return serverPresence.test(channel);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static void setSenders(BiConsumer<ServerPlayer, CustomPacketPayload> clientbound,
            Consumer<CustomPacketPayload> serverbound) {
        toClient = Objects.requireNonNull(clientbound, "clientbound");
        toServer = serverbound;
    }

    public static void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        toClient.accept(player, payload);
    }

    /** The loader's serverbound sender, or {@code null} for a plain vanilla packet. */
    public static Consumer<CustomPacketPayload> serverSender() {
        return toServer;
    }

    public static boolean isPresent(ServerPlayer player, Identifier channel) {
        try {
            return presence.test(player, channel);
        } catch (RuntimeException e) {
            // A half-closed connection must not break the sender.
            return false;
        }
    }
}

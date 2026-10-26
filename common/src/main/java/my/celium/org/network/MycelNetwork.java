package my.celium.org.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import my.celium.org.internal.NetworkHooks;
import my.celium.org.logging.MycelLog;
import my.celium.org.metadata.ModMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/**
 * Loader-independent networking.
 *
 * <p>Mods define {@link CustomPacketPayload} records, describe them with
 * {@link PacketDefinition}, and register them here. A packet each way, as in
 * the example mod:
 * <pre>{@code
 * // Common init, on both sides:
 * GREET = PacketDefinition.toClient(GreetPacket.TYPE, GreetPacket.CODEC,
 *         (packet, context) -> ClientGreeting.show(packet.message()));   // client game thread
 * MycelNetwork.register(META, GREET);
 * SEEN = PacketDefinition.toServer(SeenPacket.TYPE, SeenPacket.CODEC,
 *         (packet, context) -> MycelLog.infoMod("mymod", "{} saw it", context.sender().getName().getString()));
 * MycelNetwork.register(META, SEEN);
 *
 * // Server side, e.g. when a player joins:
 * MycelNetwork.sendToPlayer(player, GREET, new GreetPacket("Welcome back!"));
 *
 * // Client side, in ClientGreeting (never loaded on a dedicated server):
 * MycelNetwork.sendToServer(SEEN, new SeenPacket());
 * }</pre>
 * See {@link PacketDefinition} for the payload records themselves.
 *
 * <p>Sending uses plain vanilla packets on Fabric and NeoForge and Forge's
 * channel on Forge (which refuses unwrapped payloads); callers never notice.
 * Receiving is wired by each loader module (Fabric entrypoints, Forge/NeoForge
 * payload events) which iterate {@link #definitions()}; handlers always run on
 * the game thread.
 *
 * <p>Channels are optional on every loader: a player with your mod can join a
 * server without it, and the other way round. The {@code send*} methods
 * therefore skip players whose client cannot receive a channel; use
 * {@link #canSend} (or {@link #canSendToServer} on the client) to find out
 * yourself, e.g. to fall back to chat text.
 * On Forge every Mycel packet shares Mycel's own channel, so there
 * {@code canSend} reports whether the client has Mycel at all.
 *
 * <p>On Fabric, packets registered after Mycel's entrypoint ran need one extra
 * {@code FabricNetworking.flush()} call from the mod's main entrypoint and
 * {@code FabricNetworking.flushClient()} from its client entrypoint - see the
 * example mod. Both are idempotent. On Forge/NeoForge the payload event always
 * fires after every mod constructor, so nothing extra is needed.
 */
public final class MycelNetwork {
    private static final Map<String, PacketDefinition<?>> DEFINITIONS = new LinkedHashMap<>();

    private MycelNetwork() {
    }

    /**
     * Registers a packet channel. Duplicate registrations are ignored with a warning.
     * Safe to call during mod init on any loader.
     *
     * @param <T>        the payload type
     * @param mod        the owning mod
     * @param definition the packet: id, codec, direction and handler
     */
    public static synchronized <T extends CustomPacketPayload> void register(ModMetadata mod,
            PacketDefinition<T> definition) {
        Objects.requireNonNull(mod, "mod");
        Objects.requireNonNull(definition, "definition");
        String key = definition.id().toString();
        if (DEFINITIONS.containsKey(key)) {
            MycelLog.warnMod(mod.id(), "Packet channel {} already registered; ignoring duplicate", key);
            return;
        }
        if (!definition.id().getNamespace().equals(mod.id())) {
            MycelLog.warnMod(mod.id(), "Packet channel {} is not namespaced to this mod; use {}.<name>",
                    key, mod.id());
        }
        DEFINITIONS.put(key, definition);
        MycelLog.debug("Registered {} packet {}", definition.direction(), key);
    }

    /**
     * All registered definitions, in registration order. Used by the loader wiring.
     *
     * @return an unmodifiable snapshot
     * @hidden
     */
    public static synchronized List<PacketDefinition<?>> definitions() {
        return Collections.unmodifiableList(new ArrayList<>(DEFINITIONS.values()));
    }

    /**
     * Whether {@code player}'s client can receive {@code definition}'s channel,
     * i.e. has the mod installed. Call on the server thread.
     *
     * @param player     the receiving player
     * @param definition the packet
     * @return {@code true} when it would arrive
     */
    public static boolean canSend(ServerPlayer player, PacketDefinition<?> definition) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(definition, "definition");
        return NetworkHooks.isPresent(player, definition.id());
    }

    /**
     * Sends a clientbound packet to one player. Call on the server thread.
     * Does nothing when the player's client lacks the channel (see {@link #canSend}).
     *
     * @param <T>        the payload type
     * @param player     the receiving player
     * @param definition the packet, from {@link PacketDefinition#toClient}
     * @param payload    the packet to send
     */
    public static <T extends CustomPacketPayload> void sendToPlayer(ServerPlayer player,
            PacketDefinition<T> definition, T payload) {
        Objects.requireNonNull(player, "player");
        checkDirection(definition, PacketDirection.TO_CLIENT);
        if (canSend(player, definition)) {
            NetworkHooks.sendToClient(player, payload);
        }
    }

    /**
     * Sends a clientbound packet to every connected player that can receive it. Call on the server thread.
     *
     * @param <T>        the payload type
     * @param server     the running server
     * @param definition the packet, from {@link PacketDefinition#toClient}
     * @param payload    the packet to send
     */
    public static <T extends CustomPacketPayload> void sendToAll(MinecraftServer server,
            PacketDefinition<T> definition, T payload) {
        Objects.requireNonNull(server, "server");
        checkDirection(definition, PacketDirection.TO_CLIENT);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (canSend(player, definition)) {
                NetworkHooks.sendToClient(player, payload);
            }
        }
    }

    /**
     * Sends a clientbound packet to players near a chunk (same dimension, within
     * roughly view distance). Call on the server thread.
     *
     * @param <T>        the payload type
     * @param level      the dimension
     * @param pos        the chunk
     * @param definition the packet, from {@link PacketDefinition#toClient}
     * @param payload    the packet to send
     */
    public static <T extends CustomPacketPayload> void sendToTracking(ServerLevel level, ChunkPos pos,
            PacketDefinition<T> definition, T payload) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        checkDirection(definition, PacketDirection.TO_CLIENT);
        int centerX = pos.getMiddleBlockX();
        int centerZ = pos.getMiddleBlockZ();
        int rangeBlocks = Math.max(256, level.getServer().getPlayerList().getViewDistance() * 16 + 64);
        double rangeSquared = (double) rangeBlocks * rangeBlocks;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) {
                continue;
            }
            double dx = player.getX() - centerX;
            double dz = player.getZ() - centerZ;
            if (dx * dx + dz * dz <= rangeSquared && canSend(player, definition)) {
                NetworkHooks.sendToClient(player, payload);
            }
        }
    }

    /**
     * Whether the server this client is connected to has {@code definition}'s
     * channel, i.e. runs the mod. {@code false} when not connected. Client only.
     *
     * @param definition the packet
     * @return {@code true} when it would arrive
     */
    public static boolean canSendToServer(PacketDefinition<?> definition) {
        Objects.requireNonNull(definition, "definition");
        return Minecraft.getInstance().getConnection() != null && NetworkHooks.isPresentOnServer(definition.id());
    }

    /**
     * Sends a serverbound packet. Does nothing when the server lacks the
     * channel (see {@link #canSendToServer}). Must only be called from client
     * code: the method references client classes and is never invoked on a server.
     *
     * @param <T>        the payload type
     * @param definition the packet, from {@link PacketDefinition#toServer}
     * @param payload    the packet to send
     */
    public static <T extends CustomPacketPayload> void sendToServer(PacketDefinition<T> definition, T payload) {
        checkDirection(definition, PacketDirection.TO_SERVER);
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            MycelLog.warn("sendToServer dropped: not connected to a server");
            return;
        }
        if (!NetworkHooks.isPresentOnServer(definition.id())) {
            return;
        }
        var sender = NetworkHooks.serverSender();
        if (sender != null) {
            sender.accept(payload);
        } else {
            connection.send(new ServerboundCustomPayloadPacket(payload));
        }
    }

    private static void checkDirection(PacketDefinition<?> definition, PacketDirection expected) {
        Objects.requireNonNull(definition, "definition");
        if (definition.direction() != expected) {
            throw new IllegalArgumentException(
                    "Packet " + definition.id() + " is " + definition.direction() + ", expected " + expected);
        }
    }
}

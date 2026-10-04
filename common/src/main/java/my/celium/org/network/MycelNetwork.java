package my.celium.org.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import my.celium.org.logging.MycelLog;
import my.celium.org.metadata.ModMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
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
 * {@link PacketDefinition}, and register them here:
 * <pre>{@code
 * MycelNetwork.register(META, PacketDefinition.toClient(GreetPacket.TYPE, GreetPacket.CODEC, (packet, ctx) -> {
 *     // runs on the client game thread
 * }));
 *
 * MycelNetwork.sendToPlayer(player, GREET, new GreetPacket("hi"));
 * }</pre>
 *
 * <p>Sending uses plain vanilla packets, so it is identical on every loader.
 * Receiving is wired by each loader module (Fabric entrypoints, Forge/NeoForge
 * payload events) which iterate {@link #definitions()}; handlers always run on
 * the game thread.
 *
 * <p>On Fabric, packets registered after Mycel's entrypoint ran need one extra
 * {@code FabricNetworking.flush()} call from the mod's entrypoint - see the
 * example mod. On Forge/NeoForge the payload event always fires after every
 * mod constructor, so nothing extra is needed.
 */
public final class MycelNetwork {
    private static final Map<String, PacketDefinition<?>> DEFINITIONS = new LinkedHashMap<>();

    private MycelNetwork() {
    }

    /**
     * Registers a packet channel. Duplicate registrations are ignored with a warning.
     * Safe to call during mod init on any loader.
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

    /** All registered definitions, in registration order. Used by the loader wiring. */
    public static synchronized List<PacketDefinition<?>> definitions() {
        return Collections.unmodifiableList(new ArrayList<>(DEFINITIONS.values()));
    }

    /** Sends a clientbound packet to one player. Call on the server thread. */
    public static <T extends CustomPacketPayload> void sendToPlayer(ServerPlayer player,
            PacketDefinition<T> definition, T payload) {
        Objects.requireNonNull(player, "player");
        checkDirection(definition, PacketDirection.TO_CLIENT);
        player.connection.send(new ClientboundCustomPayloadPacket(payload));
    }

    /** Sends a clientbound packet to every connected player. Call on the server thread. */
    public static <T extends CustomPacketPayload> void sendToAll(MinecraftServer server,
            PacketDefinition<T> definition, T payload) {
        Objects.requireNonNull(server, "server");
        checkDirection(definition, PacketDirection.TO_CLIENT);
        ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(packet);
        }
    }

    /**
     * Sends a clientbound packet to players near a chunk (same dimension, within
     * roughly view distance). Call on the server thread.
     */
    public static <T extends CustomPacketPayload> void sendToTracking(ServerLevel level, ChunkPos pos,
            PacketDefinition<T> definition, T payload) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        checkDirection(definition, PacketDirection.TO_CLIENT);
        ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
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
            if (dx * dx + dz * dz <= rangeSquared) {
                player.connection.send(packet);
            }
        }
    }

    /**
     * Sends a serverbound packet. Must only be called from client code:
     * the method references client classes and is never invoked on a server.
     */
    public static <T extends CustomPacketPayload> void sendToServer(PacketDefinition<T> definition, T payload) {
        checkDirection(definition, PacketDirection.TO_SERVER);
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            MycelLog.warn("sendToServer dropped: not connected to a server");
            return;
        }
        connection.send(new ServerboundCustomPayloadPacket(payload));
    }

    private static void checkDirection(PacketDefinition<?> definition, PacketDirection expected) {
        Objects.requireNonNull(definition, "definition");
        if (definition.direction() != expected) {
            throw new IllegalArgumentException(
                    "Packet " + definition.id() + " is " + definition.direction() + ", expected " + expected);
        }
    }
}

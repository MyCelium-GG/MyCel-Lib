package my.celium.org.forge;

import java.util.HashSet;
import java.util.Set;

import my.celium.org.Mycel;
import my.celium.org.logging.MycelLog;
import my.celium.org.network.MycelNetwork;
import my.celium.org.network.PacketContext;
import my.celium.org.network.PacketDefinition;
import my.celium.org.network.PacketDirection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.payload.PayloadFlow;
import net.minecraftforge.network.payload.PayloadProtocol;

/**
 * Forge networking wiring over {@code ChannelBuilder.payloadChannel()}.
 *
 * <p>Mycel owns one shared play channel ({@code mycel}, protocol v1) and
 * registers every {@link MycelNetwork} definition on it. The channel is built
 * during {@code FMLCommonSetupEvent} — after every mod constructor ran, so
 * dependent mods' packets are included — and handlers are scheduled onto the
 * game thread via {@code enqueueWork}. Handler lambdas are server-safe;
 * client-only behaviour lives in the mod-provided handler.
 */
public final class ForgeNetworking {
    private static final Set<Identifier> REGISTERED = new HashSet<>();
    private static volatile boolean built;
    private static PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> serverFlow;
    private static PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> clientFlow;

    private ForgeNetworking() {
    }

    /** Hooks channel creation into mod setup. Called once from {@code MycelForge}. */
    public static void init(BusGroup modBusGroup) {
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(ForgeNetworking::onSetup);
    }

    private static void onSetup(FMLCommonSetupEvent event) {
        flushAndBuild();
    }

    private static synchronized void flushAndBuild() {
        if (built) {
            return;
        }
        PayloadProtocol<RegistryFriendlyByteBuf, CustomPacketPayload> play = ChannelBuilder
                .named(Mycel.MOD_ID)
                .networkProtocolVersion(1)
                .acceptedVersions(Channel.VersionTest.exact(1))
                .payloadChannel()
                .play();
        serverFlow = play.serverbound();
        clientFlow = play.clientbound();
        int serverCount = 0;
        int clientCount = 0;
        for (PacketDefinition<?> definition : MycelNetwork.definitions()) {
            if (definition.direction() == PacketDirection.TO_SERVER) {
                if (registerServerbound(serverFlow, definition)) {
                    serverCount++;
                }
            } else {
                if (registerClientbound(clientFlow, definition)) {
                    clientCount++;
                }
            }
        }
        if (serverCount > 0) {
            serverFlow.build();
        }
        if (clientCount > 0) {
            clientFlow.build();
        }
        built = true;
    }

    private static <T extends CustomPacketPayload> boolean registerServerbound(
            PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow, PacketDefinition<T> definition) {
        if (!REGISTERED.add(definition.id())) {
            return false;
        }
        flow.add(definition.type(), adapt(definition.codec()), (payload, context) -> context.enqueueWork(() -> {
            try {
                Player sender = context.getSender();
                ServerPlayer serverPlayer = sender instanceof ServerPlayer player ? player : null;
                MinecraftServer server = sender != null && sender.level() instanceof ServerLevel level
                        ? level.getServer()
                        : null;
                definition.handler().handle(payload,
                        new PacketContext(PacketDirection.TO_SERVER, server, serverPlayer));
            } catch (RuntimeException e) {
                MycelLog.warn("Serverbound packet {} handler threw: {}", definition.id(), e.toString());
            }
        }));
        return true;
    }

    private static <T extends CustomPacketPayload> boolean registerClientbound(
            PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow, PacketDefinition<T> definition) {
        if (!REGISTERED.add(definition.id())) {
            return false;
        }
        flow.add(definition.type(), adapt(definition.codec()), (payload, context) -> context.enqueueWork(() -> {
            try {
                definition.handler().handle(payload, new PacketContext(PacketDirection.TO_CLIENT, null, null));
            } catch (RuntimeException e) {
                MycelLog.warn("Clientbound packet {} handler threw: {}", definition.id(), e.toString());
            }
        }));
        return true;
    }

    /** Adapts a plain-buffer codec to the registry-buffer variant Forge's play phase uses. */
    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> adapt(
            StreamCodec<net.minecraft.network.FriendlyByteBuf, T> codec) {
        return StreamCodec.of(codec::encode, codec::decode);
    }
}

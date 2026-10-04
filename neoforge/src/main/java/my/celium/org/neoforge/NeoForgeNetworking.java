package my.celium.org.neoforge;

import java.util.HashSet;
import java.util.Set;

import my.celium.org.logging.MycelLog;
import my.celium.org.network.MycelNetwork;
import my.celium.org.network.PacketContext;
import my.celium.org.network.PacketDefinition;
import my.celium.org.network.PacketDirection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge networking wiring, driven by {@link RegisterPayloadHandlersEvent}.
 * Both directions are registered unconditionally: the handler lambdas are
 * server-safe (client-only work lives in the mod-provided handler, which only
 * runs on the side that receives the packet).
 */
public final class NeoForgeNetworking {
    private static final Set<Identifier> REGISTERED = new HashSet<>();

    private NeoForgeNetworking() {
    }

    public static synchronized void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        for (PacketDefinition<?> definition : MycelNetwork.definitions()) {
            if (definition.direction() == PacketDirection.TO_SERVER) {
                registerServerbound(registrar, definition);
            } else {
                registerClientbound(registrar, definition);
            }
        }
    }

    private static <T extends CustomPacketPayload> void registerServerbound(PayloadRegistrar registrar,
            PacketDefinition<T> definition) {
        if (!REGISTERED.add(definition.id())) {
            return;
        }
        registrar.playToServer(definition.type(), definition.codec(), (payload, context) -> context.enqueueWork(() -> {
            try {
                net.minecraft.world.entity.player.Player sender = context.player();
                net.minecraft.server.level.ServerPlayer serverPlayer =
                        sender instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null;
                net.minecraft.server.MinecraftServer server = sender == null ? null
                        : sender.level() instanceof net.minecraft.server.level.ServerLevel level
                                ? level.getServer()
                                : null;
                definition.handler().handle(payload,
                        new PacketContext(PacketDirection.TO_SERVER, server, serverPlayer));
            } catch (RuntimeException e) {
                MycelLog.warn("Serverbound packet {} handler threw: {}", definition.id(), e.toString());
            }
        }));
    }

    private static <T extends CustomPacketPayload> void registerClientbound(PayloadRegistrar registrar,
            PacketDefinition<T> definition) {
        if (!REGISTERED.add(definition.id())) {
            return;
        }
        registrar.playToClient(definition.type(), definition.codec(), (payload, context) -> context.enqueueWork(() -> {
            try {
                definition.handler().handle(payload, new PacketContext(PacketDirection.TO_CLIENT, null, null));
            } catch (RuntimeException e) {
                MycelLog.warn("Clientbound packet {} handler threw: {}", definition.id(), e.toString());
            }
        }));
    }
}

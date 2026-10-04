package my.celium.org.fabric;

import java.util.HashSet;
import java.util.Set;

import my.celium.org.logging.MycelLog;
import my.celium.org.network.MycelNetwork;
import my.celium.org.network.PacketContext;
import my.celium.org.network.PacketDefinition;
import my.celium.org.network.PacketDirection;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Fabric networking wiring. Public so dependent mods can re-flush after
 * registering packets past Mycel's own entrypoint (see {@link MycelNetwork}).
 */
public final class FabricNetworking {
    private static final Set<Identifier> SERVERBOUND = new HashSet<>();
    private static final Set<Identifier> CLIENTBOUND_TYPES = new HashSet<>();
    private static final Set<Identifier> CLIENTBOUND_HANDLERS = new HashSet<>();

    private FabricNetworking() {
    }

    /** Registers serverbound packets (call from the main entrypoint; idempotent). */
    public static synchronized void flush() {
        for (PacketDefinition<?> definition : MycelNetwork.definitions()) {
            if (definition.direction() == PacketDirection.TO_SERVER) {
                registerServerbound(definition);
            } else {
                registerClientboundType(definition);
            }
        }
    }

    /** Registers clientbound handlers (call from the client entrypoint; idempotent). */
    public static synchronized void flushClient() {
        for (PacketDefinition<?> definition : MycelNetwork.definitions()) {
            if (definition.direction() == PacketDirection.TO_CLIENT) {
                registerClientboundHandler(definition);
            } else {
                registerServerboundType(definition);
            }
        }
    }

    private static <T extends CustomPacketPayload> void registerServerbound(PacketDefinition<T> definition) {
        registerServerboundType(definition);
        if (!SERVERBOUND.add(definition.id())) {
            return;
        }
        ServerPlayNetworking.registerGlobalReceiver(definition.type(), (payload, context) -> {
            PacketContext ctx = new PacketContext(PacketDirection.TO_SERVER, context.server(), context.player());
            context.server().execute(() -> {
                try {
                    definition.handler().handle(payload, ctx);
                } catch (RuntimeException e) {
                    MycelLog.warn("Serverbound packet {} handler threw: {}", definition.id(), e.toString());
                }
            });
        });
    }

    private static <T extends CustomPacketPayload> void registerServerboundType(PacketDefinition<T> definition) {
        PayloadTypeRegistry.serverboundPlay().register(definition.type(), definition.codec());
    }

    private static <T extends CustomPacketPayload> void registerClientboundType(PacketDefinition<T> definition) {
        if (!CLIENTBOUND_TYPES.add(definition.id())) {
            return;
        }
        PayloadTypeRegistry.clientboundPlay().register(definition.type(), definition.codec());
    }

    private static <T extends CustomPacketPayload> void registerClientboundHandler(PacketDefinition<T> definition) {
        registerClientboundType(definition);
        if (!CLIENTBOUND_HANDLERS.add(definition.id())) {
            return;
        }
        ClientPlayNetworking.registerGlobalReceiver(definition.type(), (payload, context) -> {
            PacketContext ctx = new PacketContext(PacketDirection.TO_CLIENT, null, null);
            context.client().execute(() -> {
                try {
                    definition.handler().handle(payload, ctx);
                } catch (RuntimeException e) {
                    MycelLog.warn("Clientbound packet {} handler threw: {}", definition.id(), e.toString());
                }
            });
        });
    }
}

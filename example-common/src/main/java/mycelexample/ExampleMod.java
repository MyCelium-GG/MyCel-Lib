package mycelexample;

import io.netty.buffer.Unpooled;
import my.celium.org.Mycel;
import my.celium.org.config.ConfigBuilder;
import my.celium.org.config.ConfigValue;
import my.celium.org.config.MycelConfig;
import my.celium.org.event.MycelEvents;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.network.MycelNetwork;
import my.celium.org.network.PacketDefinition;
import my.celium.org.translation.MycelTranslations;
import my.celium.org.util.PlayerUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * A tiny QoL mod built <em>only</em> on the Mycel API: one metadata block,
 * one config, one event subscription, one packet, one translation key.
 * Roughly sixty lines of feature code — everything repetitive (file IO,
 * loader events, payload registration, screen, diagnostics) comes from Mycel.
 */
public final class ExampleMod {
    public static final String ID = "mycelexample";

    public static final ModMetadata META = ModMetadata.builder(ID, "Mycel Example", "1.0.0")
            .author("MyCelium")
            .build();

    public static ConfigValue<Boolean> enabled;
    public static ConfigValue<String> greeting;
    public static MycelConfig config;
    public static PacketDefinition<GreetPacket> greetPacket;

    private ExampleMod() {
    }

    /** Called once from each loader's entrypoint. */
    public static void init() {
        Mycel.initialize();
        Mycel.registerMod(META);

        ConfigBuilder builder = Mycel.config(META);
        enabled = builder.booleanValue("enabled", true, "Whether join greetings are sent.");
        greeting = builder.stringValue("greeting", "Welcome back, %s!", "Chat greeting; %s becomes the player name.");
        config = builder.build();

        greetPacket = PacketDefinition.toClient(GreetPacket.TYPE, GreetPacket.CODEC,
                (packet, context) -> ClientGreeting.show(packet.message()));
        MycelNetwork.register(META, greetPacket);

        MycelEvents.onPlayerJoin(event -> onJoin(event.player()));
    }

    private static void onJoin(ServerPlayer player) {
        if (!Mycel.isEnabled(ID) || !enabled.get()) {
            return;
        }
        String name = player.getName().getString();
        String fallback = String.format(greeting.get(), name);
        PlayerUtil.sendMessage(player,
                MycelTranslations.componentOrFallback(ID, "join.greeting", fallback, (Object) name));
        PlayerUtil.sendActionBar(player, MycelTranslations.component(ID, "join.actionbar"));
        MycelNetwork.sendToPlayer(player, greetPacket, new GreetPacket(fallback));
    }

    /** Clientbound packet: carries the greeting so the client can toast it. */
    public record GreetPacket(String message) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<GreetPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ID, "greet"));
        public static final StreamCodec<FriendlyByteBuf, GreetPacket> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, GreetPacket::message, GreetPacket::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /** Headless codec self-test helper (used by Mycel's own test suite pattern). */
        static GreetPacket roundTrip(GreetPacket packet) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            CODEC.encode(buf, packet);
            return CODEC.decode(buf);
        }
    }
}

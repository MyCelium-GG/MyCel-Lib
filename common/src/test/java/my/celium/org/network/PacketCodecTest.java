package my.celium.org.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

class PacketCodecTest {
    record PingPacket(String message) implements CustomPacketPayload {
        static final CustomPacketPayload.Type<PingPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("pactest", "ping"));
        static final StreamCodec<FriendlyByteBuf, PingPacket> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, PingPacket::message, PingPacket::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @Test
    void definitionCarriesIdDirectionAndCodec() {
        PacketDefinition<PingPacket> def = PacketDefinition.toServer(PingPacket.TYPE, PingPacket.CODEC,
                (packet, context) -> {
                });
        assertEquals("pactest:ping", def.id().toString());
        assertEquals(PacketDirection.TO_SERVER, def.direction());
    }

    @Test
    void payloadRoundTripsThroughTheCodec() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        PingPacket sent = new PingPacket("hello mycel");
        PingPacket.CODEC.encode(buf, sent);
        PingPacket received = PingPacket.CODEC.decode(buf);
        assertEquals(sent, received);
        buf.release();
    }
}

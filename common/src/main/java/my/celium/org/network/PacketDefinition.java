package my.celium.org.network;

import java.util.Objects;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Definition of one packet: its channel id, codec, direction and handler.
 *
 * <p>Payloads are plain {@link CustomPacketPayload} implementations owned by
 * the mod, e.g.:
 * <pre>{@code
 * public record GreetPacket(String message) implements CustomPacketPayload {
 *     public static final Type<GreetPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("mymod", "greet"));
 *     public static final StreamCodec<FriendlyByteBuf, GreetPacket> CODEC =
 *         StreamCodec.composite(ByteBufCodecs.STRING_UTF8, GreetPacket::message, GreetPacket::new);
 *
 *     @Override
 *     public Type<? extends CustomPacketPayload> type() {
 *         return TYPE;
 *     }
 * }
 * }</pre>
 *
 * @param <T> payload type
 */
public final class PacketDefinition<T extends CustomPacketPayload> {
    private final CustomPacketPayload.Type<T> type;
    private final StreamCodec<FriendlyByteBuf, T> codec;
    private final PacketDirection direction;
    private final PacketHandler<T> handler;

    private PacketDefinition(CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec,
            PacketDirection direction, PacketHandler<T> handler) {
        this.type = Objects.requireNonNull(type, "type");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.direction = Objects.requireNonNull(direction, "direction");
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    /**
     * Server {@code ->} client packet.
     *
     * @param <T>     the payload type
     * @param type    the payload's {@code TYPE} (its channel id)
     * @param codec   reads and writes the payload
     * @param handler runs on the client game thread when the packet arrives
     * @return the definition; pass it to {@link MycelNetwork#register}
     */
    public static <T extends CustomPacketPayload> PacketDefinition<T> toClient(
            CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec, PacketHandler<T> handler) {
        return new PacketDefinition<>(type, codec, PacketDirection.TO_CLIENT, handler);
    }

    /**
     * Client {@code ->} server packet.
     *
     * @param <T>     the payload type
     * @param type    the payload's {@code TYPE} (its channel id)
     * @param codec   reads and writes the payload
     * @param handler runs on the server game thread when the packet arrives
     * @return the definition; pass it to {@link MycelNetwork#register}
     */
    public static <T extends CustomPacketPayload> PacketDefinition<T> toServer(
            CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec, PacketHandler<T> handler) {
        return new PacketDefinition<>(type, codec, PacketDirection.TO_SERVER, handler);
    }

    /**
     * The payload type this packet carries.
     *
     * @return the type
     */
    public CustomPacketPayload.Type<T> type() {
        return type;
    }

    /**
     * Channel id, e.g. {@code mymod:greet}.
     *
     * @return the id
     */
    public Identifier id() {
        return type.id();
    }

    /**
     * The codec that reads and writes the payload.
     *
     * @return the codec
     */
    public StreamCodec<FriendlyByteBuf, T> codec() {
        return codec;
    }

    /**
     * Which way the packet travels.
     *
     * @return the direction
     */
    public PacketDirection direction() {
        return direction;
    }

    /**
     * What runs when the packet arrives.
     *
     * @return the handler
     */
    public PacketHandler<T> handler() {
        return handler;
    }
}

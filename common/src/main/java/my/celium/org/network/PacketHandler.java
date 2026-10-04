package my.celium.org.network;

/**
 * Handles one received packet on the game thread.
 *
 * @param <T> payload type
 */
@FunctionalInterface
public interface PacketHandler<T> {
    void handle(T packet, PacketContext context);
}

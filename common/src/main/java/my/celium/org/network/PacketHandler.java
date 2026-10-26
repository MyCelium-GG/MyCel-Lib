package my.celium.org.network;

/**
 * Handles one received packet on the game thread.
 *
 * @param <T> payload type
 */
@FunctionalInterface
public interface PacketHandler<T> {
    /**
     * Handles the packet, on the game thread.
     *
     * @param packet  the received payload
     * @param context who sent it and which way it travelled
     */
    void handle(T packet, PacketContext context);
}

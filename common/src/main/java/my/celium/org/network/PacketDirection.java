package my.celium.org.network;

/** Which way a packet travels. */
public enum PacketDirection {
    /** Client {@code ->} server. */
    TO_SERVER,
    /** Server {@code ->} client. */
    TO_CLIENT
}

package my.celium.org.event;

import net.minecraft.server.level.ServerPlayer;

/** Fired on the server thread when a player joins. */
public final class PlayerJoinEvent {
    private final ServerPlayer player;

    public PlayerJoinEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer player() {
        return player;
    }
}

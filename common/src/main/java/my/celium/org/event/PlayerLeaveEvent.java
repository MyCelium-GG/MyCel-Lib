package my.celium.org.event;

import net.minecraft.server.level.ServerPlayer;

/** Fired on the server thread when a player leaves. */
public final class PlayerLeaveEvent {
    private final ServerPlayer player;

    public PlayerLeaveEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer player() {
        return player;
    }
}

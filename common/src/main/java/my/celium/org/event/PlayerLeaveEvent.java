package my.celium.org.event;

import net.minecraft.server.level.ServerPlayer;

/** Fired on the server thread when a player leaves. */
public final class PlayerLeaveEvent {
    private final ServerPlayer player;

    /**
     * Created by the loader modules; mods only receive it.
     *
     * @param player the player
     */
    public PlayerLeaveEvent(ServerPlayer player) {
        this.player = player;
    }

    /**
     * The player who left.
     *
     * @return the player
     */
    public ServerPlayer player() {
        return player;
    }
}

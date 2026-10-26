package my.celium.org.event;

import net.minecraft.server.level.ServerPlayer;

/** Fired on the server thread when a player joins. */
public final class PlayerJoinEvent {
    private final ServerPlayer player;

    /**
     * Created by the loader modules; mods only receive it.
     *
     * @param player the player
     */
    public PlayerJoinEvent(ServerPlayer player) {
        this.player = player;
    }

    /**
     * The player who joined.
     *
     * @return the player
     */
    public ServerPlayer player() {
        return player;
    }
}

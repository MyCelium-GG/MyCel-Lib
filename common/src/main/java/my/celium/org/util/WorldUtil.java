package my.celium.org.util;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Small cohesive world/level helpers. */
public final class WorldUtil {
    private WorldUtil() {
    }

    /**
     * Whether {@code level} is the client's copy of the world.
     *
     * @param level the level
     * @return {@code true} on the client
     */
    public static boolean isClientSide(Level level) {
        return level.isClientSide();
    }

    /**
     * Whether the chunk containing {@code pos} is loaded (never forces a load).
     *
     * @param level the level
     * @param pos   any position in the chunk
     * @return {@code true} when loaded
     */
    public static boolean isChunkLoaded(Level level, BlockPos pos) {
        return level.isLoaded(pos);
    }

    /**
     * The block state at {@code pos}; same as {@link BlockUtil#getState}.
     *
     * @param level the level
     * @param pos   the position
     * @return the state
     */
    public static BlockState getBlockState(Level level, BlockPos pos) {
        return level.getBlockState(pos);
    }

    /**
     * Sets a block with default flags; same as {@link BlockUtil#setBlock}.
     *
     * @param level the level
     * @param pos   the position
     * @param state the new state
     * @return whether the block changed
     */
    public static boolean setBlock(Level level, BlockPos pos, BlockState state) {
        return BlockUtil.setBlock(level, pos, state);
    }

    /**
     * All players on a server level.
     *
     * @param level the level
     * @return its players
     */
    public static List<ServerPlayer> getPlayers(ServerLevel level) {
        return level.players();
    }

    /**
     * Current game time in ticks.
     *
     * @param level the level
     * @return the game time
     */
    public static long gameTime(ServerLevel level) {
        return level.getGameTime();
    }
}

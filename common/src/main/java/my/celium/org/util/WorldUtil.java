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

    public static boolean isClientSide(Level level) {
        return level.isClientSide();
    }

    /** Whether the chunk containing {@code pos} is loaded (never forces a load). */
    public static boolean isChunkLoaded(Level level, BlockPos pos) {
        return level.isLoaded(pos);
    }

    public static BlockState getBlockState(Level level, BlockPos pos) {
        return level.getBlockState(pos);
    }

    public static boolean setBlock(Level level, BlockPos pos, BlockState state) {
        return BlockUtil.setBlock(level, pos, state);
    }

    /** All players on a server level. */
    public static List<ServerPlayer> getPlayers(ServerLevel level) {
        return level.players();
    }

    /** Current game time in ticks. */
    public static long gameTime(ServerLevel level) {
        return level.getGameTime();
    }
}

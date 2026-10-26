package my.celium.org.util;

import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Small cohesive block helpers. Thin wrappers over {@link Level} plus the
 * one scan every QoL mod rewrites ({@link #findNearbyBlock}).
 */
public final class BlockUtil {
    private BlockUtil() {
    }

    /**
     * The block state at {@code pos}.
     *
     * @param level the level to read
     * @param pos   the position
     * @return the state there (air in unloaded or empty spots)
     */
    public static BlockState getState(Level level, BlockPos pos) {
        return level.getBlockState(pos);
    }

    /**
     * Sets a block with default flags (update neighbours + re-render).
     *
     * @param level the level to change
     * @param pos   the position
     * @param state the new state
     * @return whether the block changed
     */
    public static boolean setBlock(Level level, BlockPos pos, BlockState state) {
        return level.setBlock(pos, state, 3);
    }

    /**
     * Sets a block and notifies neighbours, like {@link Level#setBlockAndUpdate}.
     *
     * @param level the level to change
     * @param pos   the position
     * @param state the new state
     * @return whether the block changed
     */
    public static boolean setBlockAndUpdate(Level level, BlockPos pos, BlockState state) {
        return level.setBlockAndUpdate(pos, state);
    }

    /**
     * Whether the block at {@code pos} is air (any kind, cave air included).
     *
     * @param level the level to read
     * @param pos   the position
     * @return {@code true} for air
     */
    public static boolean isAir(Level level, BlockPos pos) {
        return level.getBlockState(pos).isAir();
    }

    /**
     * Whether {@code state} is air (any kind, cave air included).
     *
     * @param state the state to test
     * @return {@code true} for air
     */
    public static boolean isAir(BlockState state) {
        return state.isAir();
    }

    /**
     * Whether {@code state} holds water: water itself or a waterlogged block.
     *
     * @param state the state to test
     * @return {@code true} when its fluid is water
     */
    public static boolean isWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    /**
     * Whether {@code state} holds lava.
     *
     * @param state the state to test
     * @return {@code true} when its fluid is lava
     */
    public static boolean isLava(BlockState state) {
        return state.getFluidState().is(FluidTags.LAVA);
    }

    /**
     * Whether placing a block here would replace {@code state} (air, grass, snow layers, fluids).
     *
     * @param state the state to test
     * @return {@code true} when it can be replaced
     */
    public static boolean isReplaceable(BlockState state) {
        return state.canBeReplaced();
    }

    /**
     * Opaque full cube (redstone-blocking, vision-blocking).
     *
     * @param level the level to read
     * @param pos   the position
     * @return {@code true} for an opaque full cube
     */
    public static boolean isOpaque(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).isSolidRender();
    }

    /**
     * Finds the nearest block matching {@code predicate} in a cube around
     * {@code center}, checked closest-first. Bounded: radius is clamped to
     * {@code 1..16}. Never scans the whole world.
     *
     * @param level     the level to search
     * @param center    the middle of the cube
     * @param radius    half the cube's side, clamped to {@code 1..16}
     * @param predicate which states count as a match
     * @return the closest match, or empty when none is loaded within range
     */
    public static Optional<BlockPos> findNearbyBlock(Level level, BlockPos center, int radius,
            Predicate<BlockState> predicate) {
        int r = Math.min(16, Math.max(1, radius));
        for (int dist = 0; dist <= r; dist++) {
            for (int dx = -dist; dx <= dist; dx++) {
                for (int dy = -dist; dy <= dist; dy++) {
                    for (int dz = -dist; dz <= dist; dz++) {
                        if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != dist) {
                            continue;
                        }
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (level.isLoaded(pos) && predicate.test(level.getBlockState(pos))) {
                            return Optional.of(pos.immutable());
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }
}

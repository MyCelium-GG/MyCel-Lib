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

    public static BlockState getState(Level level, BlockPos pos) {
        return level.getBlockState(pos);
    }

    /** Sets a block with default flags (update neighbours + re-render). */
    public static boolean setBlock(Level level, BlockPos pos, BlockState state) {
        return level.setBlock(pos, state, 3);
    }

    public static boolean setBlockAndUpdate(Level level, BlockPos pos, BlockState state) {
        return level.setBlockAndUpdate(pos, state);
    }

    public static boolean isAir(Level level, BlockPos pos) {
        return level.getBlockState(pos).isAir();
    }

    public static boolean isAir(BlockState state) {
        return state.isAir();
    }

    public static boolean isWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    public static boolean isLava(BlockState state) {
        return state.getFluidState().is(FluidTags.LAVA);
    }

    public static boolean isReplaceable(BlockState state) {
        return state.canBeReplaced();
    }

    /** Opaque full cube (redstone-blocking, vision-blocking). */
    public static boolean isOpaque(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).isSolidRender();
    }

    /**
     * Finds the nearest block matching {@code predicate} in a cube around
     * {@code center}, checked closest-first. Bounded: radius is clamped to
     * {@code 1..16}. Never scans the whole world.
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

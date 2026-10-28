package my.celium.org.internal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Internal adapter for vanilla methods whose signature differs between the
 * Minecraft versions Mycel builds for. Every version has its own copy under
 * {@code common/src/mc/<version>/java}; this is the 26.3 one. All copies keep
 * the same methods.
 */
public final class MinecraftCompat {
    private MinecraftCompat() {
    }

    /** Drops {@code stack} at the player's feet, as when an inventory is full. */
    public static void dropAtFeet(ServerPlayer player, ItemStack stack) {
        player.drop(stack, false, Prediction.SERVER_ONLY);
    }

    public static boolean isPermanentlyInvulnerable(Entity entity) {
        return entity.isPermanentlyInvulnerable();
    }

    public static void setPermanentlyInvulnerable(Entity entity, boolean invulnerable) {
        entity.setPermanentlyInvulnerable(invulnerable);
    }
}

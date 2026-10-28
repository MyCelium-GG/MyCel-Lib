package my.celium.org.internal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Internal adapter for vanilla methods whose signature differs between the
 * Minecraft versions Mycel builds for. Every version has its own copy under
 * {@code common/src/mc/<version>/java}; this is the 26.2 one. All copies keep
 * the same methods.
 */
public final class MinecraftCompat {
    private MinecraftCompat() {
    }

    /** Drops {@code stack} at the player's feet, as when an inventory is full. */
    public static void dropAtFeet(ServerPlayer player, ItemStack stack) {
        // 26.2 has no Prediction yet: (stack, randomly, thrownFromHand).
        player.drop(stack, false, false);
    }

    // 26.3 split invulnerability into permanent and temporary; in 26.2 the
    // plain flag is the permanent one.
    public static boolean isPermanentlyInvulnerable(Entity entity) {
        return entity.isInvulnerable();
    }

    public static void setPermanentlyInvulnerable(Entity entity, boolean invulnerable) {
        entity.setInvulnerable(invulnerable);
    }
}

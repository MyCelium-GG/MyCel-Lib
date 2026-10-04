package my.celium.org.util;

import java.util.Set;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Small cohesive player helpers. Server-side unless noted. */
public final class PlayerUtil {
    private PlayerUtil() {
    }

    public static void sendMessage(Player player, Component message) {
        player.sendSystemMessage(message);
    }

    public static void sendActionBar(ServerPlayer player, Component message) {
        player.sendSystemMessage(message, true);
    }

    /**
     * Gives {@code stack} to the player; whatever does not fit is dropped at
     * their feet. The passed stack is consumed.
     */
    public static void giveItem(ServerPlayer player, ItemStack stack) {
        if (ItemUtil.isEmpty(stack)) {
            return;
        }
        ItemStack leftover = InventoryUtil.insert(player.getInventory(), stack.copy());
        if (!leftover.isEmpty()) {
            player.drop(leftover, false, Prediction.SERVER_ONLY);
        }
    }

    public static boolean hasItem(Player player, Item item, int amount) {
        return InventoryUtil.contains(player.getInventory(), item, amount);
    }

    public static int countItem(Player player, Item item) {
        return InventoryUtil.countItem(player.getInventory(), item);
    }

    public static int removeItem(Player player, Item item, int amount) {
        return InventoryUtil.removeItem(player.getInventory(), item, amount);
    }

    /** Unit look direction vector. */
    public static Vec3 getLookDirection(Player player) {
        return player.getLookAngle();
    }

    /** Plays a sound audible around the player (vanilla radius rules apply). */
    public static void playSound(ServerLevel level, Player player, SoundEvent event, SoundSource source,
            float volume, float pitch) {
        level.playSound(null, player.blockPosition(), event, source, volume, pitch);
    }

    /**
     * Teleports within the current dimension and syncs the client.
     * For cross-dimension travel see {@link #teleportToLevel}.
     */
    public static void teleport(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
        player.connection.teleport(x, y, z, yaw, pitch);
    }

    /**
     * Teleports to another dimension (same dimension works too).
     *
     * @return whether the teleport succeeded
     */
    public static boolean teleportToLevel(ServerPlayer player, ServerLevel target, double x, double y, double z,
            float yaw, float pitch) {
        return player.teleportTo(target, x, y, z, Set.<Relative>of(), yaw, pitch, true);
    }

    /** Compatibility overload taking a position vector. */
    public static boolean teleportToLevel(ServerPlayer player, ServerLevel target, Vec3 pos, float yaw, float pitch) {
        return teleportToLevel(player, target, pos.x(), pos.y(), pos.z(), yaw, pitch);
    }
}

package my.celium.org.util;

import java.util.Set;

import my.celium.org.internal.MinecraftCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Small cohesive player helpers. Server-side unless noted. */
public final class PlayerUtil {
    private PlayerUtil() {
    }

    /**
     * Sends a chat message to {@code player}.
     *
     * @param player the player
     * @param message the message
     */
    public static void sendMessage(Player player, Component message) {
        player.sendSystemMessage(message);
    }

    /**
     * Shows {@code message} above the hotbar instead of in chat.
     *
     * @param player the player
     * @param message the message
     */
    public static void sendActionBar(ServerPlayer player, Component message) {
        player.sendSystemMessage(message, true);
    }

    /**
     * Gives {@code stack} to the player; whatever does not fit is dropped at
     * their feet. The passed stack is consumed.
     *
     * @param player the player
     * @param stack  the items
     */
    public static void giveItem(ServerPlayer player, ItemStack stack) {
        if (ItemUtil.isEmpty(stack)) {
            return;
        }
        ItemStack leftover = InventoryUtil.insert(player.getInventory(), stack.copy());
        if (!leftover.isEmpty()) {
            MinecraftCompat.dropAtFeet(player, leftover);
        }
    }

    /**
     * Whether {@code player} carries at least {@code amount} of {@code item}.
     *
     * @param player the player
     * @param item   the item
     * @param amount how many are needed
     * @return {@code true} when there are enough
     */
    public static boolean hasItem(Player player, Item item, int amount) {
        return InventoryUtil.contains(player.getInventory(), item, amount);
    }

    /**
     * How many of {@code item} {@code player} carries.
     *
     * @param player the player
     * @param item   the item
     * @return the total count
     */
    public static int countItem(Player player, Item item) {
        return InventoryUtil.countItem(player.getInventory(), item);
    }

    /**
     * Takes up to {@code amount} of {@code item} from {@code player}'s inventory.
     *
     * @param player the player
     * @param item   the item
     * @param amount the most to remove
     * @return how many were removed
     */
    public static int removeItem(Player player, Item item, int amount) {
        return InventoryUtil.removeItem(player.getInventory(), item, amount);
    }

    /**
     * Unit look direction vector.
     *
     * @param player the player
     * @return a vector of length 1
     */
    public static Vec3 getLookDirection(Player player) {
        return player.getLookAngle();
    }

    /**
     * Plays a sound audible around the player (vanilla radius rules apply).
     *
     * @param level  the level the player is in
     * @param player the player
     * @param event  the sound
     * @param source its volume slider (e.g. {@code SoundSource.PLAYERS})
     * @param volume the volume, {@code 1.0} is normal
     * @param pitch  the pitch, {@code 1.0} is normal
     */
    public static void playSound(ServerLevel level, Player player, SoundEvent event, SoundSource source,
            float volume, float pitch) {
        level.playSound(null, player.blockPosition(), event, source, volume, pitch);
    }

    /**
     * Teleports within the current dimension and syncs the client.
     * For cross-dimension travel see {@link #teleportToLevel}.
     *
     * @param player the player
     * @param x      the target x
     * @param y      the target y
     * @param z      the target z
     * @param yaw    the facing, in degrees
     * @param pitch  up/down, in degrees
     */
    public static void teleport(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
        player.connection.teleport(x, y, z, yaw, pitch);
    }

    /**
     * Teleports to another dimension (same dimension works too).
     *
     * @param player the player
     * @param target the dimension to go to
     * @param x      the target x
     * @param y      the target y
     * @param z      the target z
     * @param yaw    the facing, in degrees
     * @param pitch  up/down, in degrees
     * @return whether the teleport succeeded
     */
    public static boolean teleportToLevel(ServerPlayer player, ServerLevel target, double x, double y, double z,
            float yaw, float pitch) {
        return player.teleportTo(target, x, y, z, Set.<Relative>of(), yaw, pitch, true);
    }

    /**
     * Teleports to another dimension (same dimension works too).
     *
     * @param player the player
     * @param target the dimension to go to
     * @param pos    the target position
     * @param yaw    the facing, in degrees
     * @param pitch  up/down, in degrees
     * @return whether the teleport succeeded
     */
    public static boolean teleportToLevel(ServerPlayer player, ServerLevel target, Vec3 pos, float yaw, float pitch) {
        return teleportToLevel(player, target, pos.x(), pos.y(), pos.z(), yaw, pitch);
    }
}

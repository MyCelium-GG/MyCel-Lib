package my.celium.org.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Small cohesive item-stack helpers. */
public final class ItemUtil {
    private ItemUtil() {
    }

    /**
     * Whether {@code stack} is empty or {@code null}.
     *
     * @param stack the stack, may be {@code null}
     * @return {@code true} when empty
     */
    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }

    /**
     * How many items {@code stack} holds.
     *
     * @param stack the stack, may be {@code null}
     * @return the count, {@code 0} when empty
     */
    public static int count(ItemStack stack) {
        return isEmpty(stack) ? 0 : stack.getCount();
    }

    /**
     * Same item including components (enchantments, names, ...).
     *
     * @param a one stack
     * @param b the other stack
     * @return {@code true} when both are non-empty and the same; counts may differ
     */
    public static boolean matches(ItemStack a, ItemStack b) {
        if (isEmpty(a) || isEmpty(b)) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(a, b);
    }

    /**
     * Whether {@code stack} is a non-empty stack of {@code item}.
     *
     * @param stack the stack, may be {@code null}
     * @param item  the item
     * @return {@code true} on a match
     */
    public static boolean matchesItem(ItemStack stack, Item item) {
        return !isEmpty(stack) && stack.is(item);
    }

    /**
     * A copy of {@code stack} holding {@code count} items.
     *
     * @param stack the stack to copy
     * @param count the new count
     * @return the copy
     */
    public static ItemStack copyWithCount(ItemStack stack, int count) {
        return stack.copyWithCount(count);
    }

    /**
     * Custom name when present, otherwise the default item name.
     *
     * @param stack the stack
     * @return the name as shown in tooltips
     */
    public static Component displayName(ItemStack stack) {
        return stack.getHoverName();
    }

    /**
     * Damages a stack on the server (armour, tools, ...). No-op for empty or
     * undamageable stacks.
     *
     * @param level  the level the player is in
     * @param stack  the stack to damage; breaks like in vanilla when used up
     * @param player who used it
     * @param amount how much durability to take
     */
    public static void damage(ServerLevel level, ItemStack stack, ServerPlayer player, int amount) {
        if (isEmpty(stack) || !stack.isDamageableItem() || amount <= 0) {
            return;
        }
        stack.hurtAndBreak(amount, level, player, item -> {
        });
    }
}

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

    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }

    public static int count(ItemStack stack) {
        return isEmpty(stack) ? 0 : stack.getCount();
    }

    /** Same item including components (enchantments, names, ...). */
    public static boolean matches(ItemStack a, ItemStack b) {
        if (isEmpty(a) || isEmpty(b)) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(a, b);
    }

    public static boolean matchesItem(ItemStack stack, Item item) {
        return !isEmpty(stack) && stack.is(item);
    }

    public static ItemStack copyWithCount(ItemStack stack, int count) {
        return stack.copyWithCount(count);
    }

    /** Custom name when present, otherwise the default item name. */
    public static Component displayName(ItemStack stack) {
        return stack.getHoverName();
    }

    /**
     * Damages a stack on the server (armour, tools, ...). No-op for empty or
     * undamageable stacks.
     */
    public static void damage(ServerLevel level, ItemStack stack, ServerPlayer player, int amount) {
        if (isEmpty(stack) || !stack.isDamageableItem() || amount <= 0) {
            return;
        }
        stack.hurtAndBreak(amount, level, player, item -> {
        });
    }
}

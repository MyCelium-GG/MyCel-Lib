package my.celium.org.util;

import java.util.function.Predicate;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Small cohesive inventory helpers over vanilla {@link Container}.
 * Slot counts are small; all scans are linear and allocation-free in spirit.
 */
public final class InventoryUtil {
    private InventoryUtil() {
    }

    public static int count(Container container, Predicate<ItemStack> filter) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && filter.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static int count(Player player, Predicate<ItemStack> filter) {
        return count(player.getInventory(), filter);
    }

    public static int countItem(Container container, Item item) {
        return count(container, stack -> stack.is(item));
    }

    public static boolean contains(Container container, Item item, int amount) {
        return countItem(container, item) >= amount;
    }

    /**
     * Removes up to {@code amount} matching items. Returns how many were
     * actually removed.
     */
    public static int remove(Container container, Predicate<ItemStack> filter, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || !filter.test(stack)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
            if (stack.isEmpty()) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
        if (remaining < amount) {
            container.setChanged();
        }
        return amount - remaining;
    }

    public static int removeItem(Container container, Item item, int amount) {
        return remove(container, stack -> stack.is(item), amount);
    }

    /**
     * Inserts {@code stack}, merging with existing stacks first. Returns the
     * leftover (possibly empty). The passed stack is consumed.
     */
    public static ItemStack insert(Container container, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        int size = container.getContainerSize();
        // Merge first.
        for (int slot = 0; slot < size && !stack.isEmpty(); slot++) {
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) {
                continue;
            }
            int space = Math.min(existing.getMaxStackSize(), container.getMaxStackSize()) - existing.getCount();
            if (space <= 0) {
                continue;
            }
            int move = Math.min(space, stack.getCount());
            existing.grow(move);
            stack.shrink(move);
        }
        // Then empty slots.
        for (int slot = 0; slot < size && !stack.isEmpty(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                int limit = Math.min(stack.getMaxStackSize(), container.getMaxStackSize());
                container.setItem(slot, stack.copyWithCount(Math.min(limit, stack.getCount())));
                stack.shrink(Math.min(limit, stack.getCount()));
            }
        }
        container.setChanged();
        return stack;
    }

    /** First slot matching the filter, or {@code -1}. */
    public static int findSlot(Container container, Predicate<ItemStack> filter) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && filter.test(stack)) {
                return slot;
            }
        }
        return -1;
    }

    /** Splits one item off into the player's hand area (used by QoL interactions). */
    public static boolean holdOne(Player player, Predicate<ItemStack> filter) {
        Inventory inventory = player.getInventory();
        int slot = findSlot(inventory, filter);
        return slot >= 0;
    }
}

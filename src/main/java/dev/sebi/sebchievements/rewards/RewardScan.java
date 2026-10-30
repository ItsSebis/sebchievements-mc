package dev.sebi.sebchievements.rewards;

import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Finds reward copies a player holds: cursor, inventory (armor and offhand included) and ender chest, plus whatever
 * sits inside shulker boxes and bundles they carry. Counts are keyed by {@link RewardLimit#key}.
 */
public final class RewardScan {
    private RewardScan() {}

    /** Reward copies held by the player. */
    public static Map<String, Integer> held(Player p) {
        Map<String, Integer> out = new HashMap<>();
        walk(p, s -> {
            add(out, s);
            return s;
        });
        return out;
    }

    /** Reward copies in one stack, including its contents when it is a shulker box or bundle. */
    public static Map<String, Integer> inStack(ItemStack stack) {
        Map<String, Integer> out = new HashMap<>();
        walkStack(stack.clone(), s -> {
            add(out, s);
            return s;
        });
        return out;
    }

    /** Removes up to {@code excess} (per key) reward copies from the player; returns how many were removed. */
    public static int remove(Player p, Map<String, Integer> excess) {
        Map<String, Integer> left = new HashMap<>(excess);
        int[] removed = {0};
        walk(p, s -> {
            String id = RewardItems.idOf(s);
            if (id == null) return s;
            String key = RewardLimit.key(id, RewardItems.itemOf(s));
            int open = left.getOrDefault(key, 0);
            if (open <= 0) return s;
            int take = Math.min(open, s.getAmount());
            left.put(key, open - take);
            removed[0] += take;
            return take >= s.getAmount() ? null : s.asQuantity(s.getAmount() - take);
        });
        return removed[0];
    }

    private static void add(Map<String, Integer> out, ItemStack s) {
        String id = RewardItems.idOf(s);
        if (id != null) out.merge(RewardLimit.key(id, RewardItems.itemOf(s)), s.getAmount(), Integer::sum);
    }

    /**
     * Applies {@code fn} to every stack the player holds and writes back what changed. Order: cursor, inventory, ender
     * chest, so the ender chest copy is the last to go. {@code fn} returns the same instance when it changes nothing,
     * a replacement otherwise, or null to delete the stack.
     */
    private static void walk(Player p, UnaryOperator<ItemStack> fn) {
        ItemStack cursor = p.getItemOnCursor();
        ItemStack newCursor = walkStack(cursor, fn);
        if (newCursor != cursor) p.setItemOnCursor(newCursor);
        walkInventory(p.getInventory(), fn);
        walkInventory(p.getEnderChest(), fn);
    }

    private static void walkInventory(Inventory inv, UnaryOperator<ItemStack> fn) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack before = inv.getItem(i);
            ItemStack after = walkStack(before, fn);
            if (after != before) inv.setItem(i, after);
        }
    }

    /** Recurses into shulker boxes and bundles first, then applies {@code fn} to the stack itself. */
    private static ItemStack walkStack(ItemStack stack, UnaryOperator<ItemStack> fn) {
        if (stack == null || stack.getType().isAir()) return stack;
        if (stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta instanceof BlockStateMeta block && block.hasBlockState()
                    && block.getBlockState() instanceof Container box) {
                Inventory inside = box.getInventory();
                boolean changed = false;
                for (int i = 0; i < inside.getSize(); i++) {
                    ItemStack before = inside.getItem(i);
                    ItemStack after = walkStack(before, fn);
                    if (after != before) {
                        inside.setItem(i, after);
                        changed = true;
                    }
                }
                if (changed) {
                    block.setBlockState(box);
                    stack.setItemMeta(block);
                }
            } else if (meta instanceof BundleMeta bundle && bundle.hasItems()) {
                List<ItemStack> items = new ArrayList<>();
                boolean changed = false;
                for (ItemStack before : bundle.getItems()) {
                    ItemStack after = walkStack(before, fn);
                    if (after != before) changed = true;
                    if (after != null) items.add(after);
                }
                if (changed) {
                    bundle.setItems(items);
                    stack.setItemMeta(bundle);
                }
            }
        }
        return fn.apply(stack);
    }
}

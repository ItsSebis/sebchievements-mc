package dev.sebi.sebchievements.rewards;

import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps players without {@code smpadv.rewards.multiple} at their allowance of each reward: picking an extra copy up,
 * taking one out of a container, and a sweep that removes whatever slipped past (every {@code TickScheduler} interval).
 */
public final class RewardGuard implements Listener {
    private static final Set<InventoryAction> TAKES = EnumSet.of(InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE, InventoryAction.PICKUP_SOME, InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.HOTBAR_SWAP, InventoryAction.SWAP_WITH_CURSOR);
    /** Chat message at most this often per player, so standing on an item doesn't spam. */
    private static final long WARN_EVERY_MILLIS = 5_000;

    private final RewardService service;
    private final Map<UUID, Long> lastWarn = new HashMap<>();

    public RewardGuard(RewardService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(PlayerAttemptPickupItemEvent e) {
        Player p = e.getPlayer();
        if (service.unlimited(p) || service.canReceive(p, e.getItem().getItemStack())) return;
        e.setCancelled(true);
        warn(p, "You can't carry another copy of that reward.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || service.unlimited(p)) return;
        Inventory clicked = e.getClickedInventory();
        // moves inside the player's own inventory or from/to their ender chest don't change what they hold
        if (clicked == null || clicked.equals(p.getInventory()) || clicked.equals(p.getEnderChest())) return;
        ItemStack taken = e.getCurrentItem();
        if (taken == null || !TAKES.contains(e.getAction()) || service.canReceive(p, taken)) return;
        e.setCancelled(true);
        warn(p, "You can't take another copy of that reward.");
    }

    /** Runs from the shared TickScheduler. */
    public void sweep(Player p) {
        if (service.unlimited(p)) return;
        int removed = service.enforce(p);
        if (removed > 0) {
            p.updateInventory();
            p.sendMessage(Component.text("Removed " + removed + " extra reward copy item(s) you were not allowed to carry.",
                    NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastWarn.remove(e.getPlayer().getUniqueId());
    }

    private void warn(Player p, String text) {
        long now = System.currentTimeMillis();
        Long last = lastWarn.get(p.getUniqueId());
        if (last != null && now - last < WARN_EVERY_MILLIS) return;
        lastWarn.put(p.getUniqueId(), now);
        p.sendMessage(Component.text(text, NamedTextColor.RED));
    }
}

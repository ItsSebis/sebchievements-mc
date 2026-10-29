package dev.sebi.sebchievements.rewards;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Routes clicks in a {@link RewardMenu}; nothing can be moved in or out of it. */
public final class RewardMenuListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof RewardMenu menu)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == e.getView().getTopInventory()) menu.click(e.getSlot());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof RewardMenu) e.setCancelled(true);
    }
}

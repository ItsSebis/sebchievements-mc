package dev.sebi.sebchievements.rewards;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Routes clicks in a {@link RewardMenu} or {@link PerkMenu}; nothing can be moved in or out of it. */
public final class RewardMenuListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        var holder = e.getView().getTopInventory().getHolder();
        if (!(holder instanceof RewardMenu) && !(holder instanceof PerkMenu)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        if (holder instanceof RewardMenu menu) menu.click(e.getSlot());
        else if (holder instanceof PerkMenu menu) menu.click(e.getSlot());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent e) {
        var holder = e.getView().getTopInventory().getHolder();
        if (holder instanceof RewardMenu || holder instanceof PerkMenu) e.setCancelled(true);
    }
}

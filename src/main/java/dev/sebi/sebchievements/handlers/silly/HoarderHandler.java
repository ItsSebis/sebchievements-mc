package dev.sebi.sebchievements.handlers.silly;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.Material;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** On closing a double chest: every slot holds a full stack of the configured material (dirt). */
public final class HoarderHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;
    private Material material;

    @Override
    public String id() {
        return AdvancementIds.HOARDER;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        Material m = Material.matchMaterial(ctx.config().getString("material", "DIRT"));
        this.material = m != null ? m : Material.DIRT;
        ctx.listen(this);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (!(inv.getHolder() instanceof DoubleChest) || !(e.getPlayer() instanceof Player p)) return;
        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() != material || item.getAmount() != item.getMaxStackSize()) return;
        }
        ctx.grant(p);
    }
}

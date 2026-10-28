package dev.sebi.sebchievements.handlers.silly;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/** Die of fall damage while a water bucket sits in the hotbar (slots 0-8; the offhand does not count). */
public final class GravityCheckHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.GRAVITY_CHECK;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        EntityDamageEvent last = p.getLastDamageCause();
        if (last == null || last.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack item = p.getInventory().getItem(slot);
            if (item != null && item.getType() == Material.WATER_BUCKET) {
                ctx.grant(p);
                return;
            }
        }
    }
}

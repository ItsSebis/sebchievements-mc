package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/** The last damage to the Ender Dragon was a melee hit by the killer holding a wooden sword. */
public final class WhittledDownHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.WHITTLED_DOWN;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof EnderDragon dragon)) return;
        Player killer = dragon.getKiller();
        if (killer == null) return;
        if (dragon.getLastDamageCause() instanceof EntityDamageByEntityEvent hit && hit.getDamager().equals(killer)
                && killer.getInventory().getItemInMainHand().getType() == Material.WOODEN_SWORD) {
            ctx.grant(killer);
        }
    }
}

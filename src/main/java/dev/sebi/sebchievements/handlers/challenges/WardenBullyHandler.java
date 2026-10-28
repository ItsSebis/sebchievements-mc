package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.handlers.support.Armor;
import dev.sebi.sebchievements.services.ParticipationTracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

/**
 * Per Warden: a player is disqualified for it by wearing armor during any hit, or by damaging it with a projectile.
 * When a player kills it, every player who hurt it and stayed qualified gets the advancement.
 */
public final class WardenBullyHandler implements AdvancementHandler, Listener {
    private final ParticipationTracker wardens = new ParticipationTracker();
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.WARDEN_BULLY;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Warden warden)) return;
        if (e.getDamager() instanceof Player p) {
            wardens.hit(warden.getUniqueId(), p.getUniqueId(), !Armor.wearsAny(p));
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Entity shooter && shooter instanceof Player p) {
            wardens.hit(warden.getUniqueId(), p.getUniqueId(), false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Warden warden)) return;
        if (warden.getKiller() != null) {
            for (UUID id : wardens.qualified(warden.getUniqueId())) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) ctx.grant(p);
            }
        }
        wardens.forget(warden.getUniqueId());
    }
}

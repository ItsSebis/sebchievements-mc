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
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

/**
 * Per Wither: a player is disqualified for it the first time they damage it while wearing armor. When a player
 * kills it, every player who hurt it and never wore armor while doing so gets the advancement.
 */
public final class NakedAndAfraidHandler implements AdvancementHandler, Listener {
    private final ParticipationTracker withers = new ParticipationTracker();
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.NAKED_AND_AFRAID;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Wither wither)) return;
        Entity source = e.getDamager();
        if (source instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) source = shooter;
        if (source instanceof Player p) withers.hit(wither.getUniqueId(), p.getUniqueId(), !Armor.wearsAny(p));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Wither wither)) return;
        if (wither.getKiller() != null) {
            for (UUID id : withers.qualified(wither.getUniqueId())) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && !Armor.wearsAny(p)) ctx.grant(p);
            }
        }
        withers.forget(wither.getUniqueId());
    }
}

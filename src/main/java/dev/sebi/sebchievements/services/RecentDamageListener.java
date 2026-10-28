package dev.sebi.sebchievements.services;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Feeds {@link RecentDamageService}. Projectiles are attributed to their shooter when it is an entity. */
public final class RecentDamageListener implements Listener {
    private final RecentDamageService service;

    public RecentDamageListener(RecentDamageService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        Entity source = e.getDamager();
        if (source instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) source = shooter;
        service.record(victim.getUniqueId(), source.getUniqueId(), e.getFinalDamage(), System.currentTimeMillis());
    }
}

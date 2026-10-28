package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Three criteria on one advancement (chicken_jockey, spider_jockey, skeleton_horseman). The rider's mount is
 * snapshotted at every player hit, because a dying rider may already be dismounted when the death event fires.
 */
public final class JockeyJamboreeHandler implements AdvancementHandler, Listener {
    private record Mounted(String criterion, UUID player) {}

    private static final int MAX_SNAPSHOTS = 256;

    private final Map<UUID, Mounted> snapshots = new HashMap<>();
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.JOCKEY_JAMBOREE;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    private static String criterionOf(LivingEntity rider) {
        Entity mount = rider.getVehicle();
        if (rider instanceof Zombie && mount instanceof Chicken) return "chicken_jockey";
        if (rider instanceof AbstractSkeleton && mount instanceof Spider) return "spider_jockey";
        if (rider instanceof AbstractSkeleton && mount instanceof SkeletonHorse) return "skeleton_horseman";
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        Entity source = e.getDamager();
        if (source instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) source = shooter;
        if (!(source instanceof Player p)) return;
        String criterion = criterionOf(victim);
        if (criterion == null) {
            snapshots.remove(victim.getUniqueId());
            return;
        }
        if (snapshots.size() >= MAX_SNAPSHOTS) snapshots.clear();
        snapshots.put(victim.getUniqueId(), new Mounted(criterion, p.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        Mounted m = snapshots.remove(e.getEntity().getUniqueId());
        Player killer = e.getEntity().getKiller();
        if (m == null || killer == null || !killer.getUniqueId().equals(m.player())) return;
        ctx.grantCriterion(killer, m.criterion());
    }
}

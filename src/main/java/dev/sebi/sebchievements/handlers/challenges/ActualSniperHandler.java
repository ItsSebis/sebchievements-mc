package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/** The shooter's position is stamped on the arrow (PDC) at launch; the kill is measured from there. */
public final class ActualSniperHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;
    private NamespacedKey originKey;

    @Override
    public String id() {
        return AdvancementIds.ACTUAL_SNIPER;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.originKey = new NamespacedKey(ctx.plugin(), "sniper_origin");
        ctx.listen(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof AbstractArrow arrow) || arrow instanceof Trident || !(arrow.getShooter() instanceof Player shooter)) return;
        Location l = shooter.getLocation();
        arrow.getPersistentDataContainer().set(originKey, PersistentDataType.STRING,
                l.getWorld().getUID() + ";" + l.getX() + ";" + l.getY() + ";" + l.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || !(victim instanceof Mob)) return;
        if (!(victim.getLastDamageCause() instanceof EntityDamageByEntityEvent hit)
                || !(hit.getDamager() instanceof AbstractArrow arrow) || !killer.equals(arrow.getShooter())) return;
        String raw = arrow.getPersistentDataContainer().get(originKey, PersistentDataType.STRING);
        if (raw == null) return;
        String[] parts = raw.split(";");
        World world = Bukkit.getWorld(UUID.fromString(parts[0]));
        if (world == null || !world.equals(victim.getWorld())) return;
        Location origin = new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
        if (origin.distance(victim.getLocation()) >= ctx.config().getDouble("blocks", 100)) ctx.grant(killer);
    }
}

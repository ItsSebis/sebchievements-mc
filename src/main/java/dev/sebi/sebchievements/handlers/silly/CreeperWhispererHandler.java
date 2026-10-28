package dev.sebi.sebchievements.handlers.silly;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Candidates are recorded when the creeper explodes; anyone who then takes final damage greater than zero
 * from that creeper is removed (a blocking shield makes final damage 0, so it still counts). The grant happens
 * on the next tick, after the explosion's damage events.
 */
public final class CreeperWhispererHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;
    private final Map<UUID, Set<UUID>> candidates = new HashMap<>();

    @Override
    public String id() {
        return AdvancementIds.CREEPER_WHISPERER;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (!(e.getEntity() instanceof Creeper creeper)) return;
        double r = ctx.config().getDouble("radius", 3.0);
        Set<UUID> near = new HashSet<>();
        for (Player p : creeper.getWorld().getPlayers()) {
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
            if (p.getLocation().distanceSquared(creeper.getLocation()) <= r * r) near.add(p.getUniqueId());
        }
        if (near.isEmpty()) return;
        UUID creeperId = creeper.getUniqueId();
        candidates.put(creeperId, near);
        Bukkit.getScheduler().runTask(ctx.plugin(), () -> {
            Set<UUID> survivors = candidates.remove(creeperId);
            if (survivors == null) return;
            for (UUID id : survivors) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && !p.isDead()) ctx.grant(p);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Creeper c) || !(e.getEntity() instanceof Player p)) return;
        Set<UUID> set = candidates.get(c.getUniqueId());
        if (set != null && (e.isCancelled() ? 0 : e.getFinalDamage()) > 0) set.remove(p.getUniqueId());
    }
}

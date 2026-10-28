package dev.sebi.sebchievements.handlers.social;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

/** Kill a mob that damaged some other player within the last few seconds (RecentDamageService). */
public final class BodyguardHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.BODYGUARD;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || !(victim instanceof Mob)) return;
        long within = ctx.config().getLong("seconds", 5) * 1000;
        for (UUID hurt : ctx.services().damage().victimsOf(victim.getUniqueId(), within, System.currentTimeMillis())) {
            Player other = Bukkit.getPlayer(hurt);
            if (other != null && !other.equals(killer)) {
                ctx.grant(killer);
                return;
            }
        }
    }
}

package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.handlers.support.TimedHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

/** No damage for the configured time. Any damage that gets through (final damage above zero) resets the timer. */
public final class UntouchableHandler extends TimedHandler {
    @Override
    public String id() {
        return AdvancementIds.UNTOUCHABLE;
    }

    @Override
    protected boolean conditionHolds(Player p) {
        return !p.isDead();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && e.getFinalDamage() > 0) {
            ctx().services().timers().reset(p.getUniqueId(), ctx().id());
        }
    }
}

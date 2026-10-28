package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/** The killer of N Withers within the window. */
public final class WitherStormHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.WITHER_STORM;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Wither wither)) return;
        Player killer = wither.getKiller();
        if (killer == null) return;
        long window = ctx.config().getLong("seconds", 600) * 1000;
        int needed = ctx.config().getInt("withers", 3);
        if (ctx.services().windows().record(killer.getUniqueId(), ctx.id(), wither.getUniqueId().toString(), System.currentTimeMillis(), window) >= needed) {
            ctx.grant(killer);
        }
    }
}

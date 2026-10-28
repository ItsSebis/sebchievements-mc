package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;

import java.util.UUID;

/** N totem pops inside the window; every pop is its own distinct event. */
public final class TotemChainHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.TOTEM_CHAIN;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPop(EntityResurrectEvent e) {
        if (!(e.getEntity() instanceof Player p) || e.getHand() == null) return;
        long window = ctx.config().getLong("seconds", 60) * 1000;
        int needed = ctx.config().getInt("totems", 3);
        if (ctx.services().windows().record(p.getUniqueId(), ctx.id(), UUID.randomUUID().toString(), System.currentTimeMillis(), window) >= needed) {
            ctx.grant(p);
        }
    }
}

package dev.sebi.sebchievements.handlers.exploration;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

/** Each dimension change records both the dimension left and the one entered; all three inside the window grants. */
public final class DimensionHopperHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.DIMENSION_HOPPER;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler
    public void onChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        World.Environment from = e.getFrom().getEnvironment(), to = p.getWorld().getEnvironment();
        if (from == World.Environment.CUSTOM || to == World.Environment.CUSTOM) return;
        long now = System.currentTimeMillis(), window = ctx.config().getLong("seconds", 600) * 1000;
        ctx.services().windows().record(p.getUniqueId(), ctx.id(), from.name(), now, window);
        if (ctx.services().windows().record(p.getUniqueId(), ctx.id(), to.name(), now, window) >= 3) ctx.grant(p);
    }
}

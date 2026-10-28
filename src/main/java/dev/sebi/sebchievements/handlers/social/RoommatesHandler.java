package dev.sebi.sebchievements.handlers.social;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

/** Sleep within N blocks of another sleeping player; both get it. */
public final class RoommatesHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.ROOMMATES;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent e) {
        if (e.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;
        Player entering = e.getPlayer();
        // the player is only "sleeping" once the event has finished
        Bukkit.getScheduler().runTask(ctx.plugin(), () -> {
            if (!entering.isOnline() || !entering.isSleeping()) return;
            double radius = ctx.config().getDouble("radius", 5);
            for (Player other : ctx.services().proximity().nearby(entering, radius)) {
                if (!other.isSleeping()) continue;
                ctx.grant(entering);
                ctx.grant(other);
            }
        });
    }
}

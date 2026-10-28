package dev.sebi.sebchievements.handlers.exploration;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.handlers.support.TimedHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.services.GameClock;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Be above Y (default 200) for the whole night. Counts game ticks inside the night window, so lag does not
 * matter; being below the height, leaving the night window (including a skipped night) or logging out resets it.
 */
public final class SkyCamperHandler extends TimedHandler {
    private final GameClock clock = new GameClock();

    @Override
    public String id() {
        return AdvancementIds.SKY_CAMPER;
    }

    @Override
    protected long requiredMillis(HandlerContext ctx) {
        long night = ctx.config().getLong("night_end", 23000) - ctx.config().getLong("night_start", 13000);
        return (night - ctx.config().getLong("slack_ticks", 40)) * 50;
    }

    @Override
    protected long deltaMillis(Player p) {
        return clock.deltaTicks(p.getUniqueId(), p.getWorld().getUID(), p.getWorld().getFullTime(),
                ctx().config().getLong("max_jump_ticks", 400)) * 50;
    }

    @Override
    protected boolean conditionHolds(Player p) {
        World w = p.getWorld();
        long time = w.getTime();
        return w.getEnvironment() == World.Environment.NORMAL
                && time >= ctx().config().getLong("night_start", 13000) && time < ctx().config().getLong("night_end", 23000)
                && p.getLocation().getY() >= ctx().config().getDouble("y", 200);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        clock.forget(e.getPlayer().getUniqueId());
        ctx().services().timers().reset(e.getPlayer().getUniqueId(), ctx().id());
    }
}

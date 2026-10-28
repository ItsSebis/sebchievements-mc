package dev.sebi.sebchievements.handlers.exploration;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.handlers.support.TimedHandler;
import dev.sebi.sebchievements.services.GameClock;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

/** A full in-game day (24000 ticks) with sky light 0 at the player's head. Overworld only. */
public final class CavemanHandler extends TimedHandler {
    private final GameClock clock = new GameClock();

    @Override
    public String id() {
        return AdvancementIds.CAVEMAN;
    }

    @Override
    protected long requiredMillis(HandlerContext ctx) {
        return ctx.config().getLong("ticks", 24000) * 50;
    }

    @Override
    protected long deltaMillis(Player p) {
        return clock.deltaTicks(p.getUniqueId(), p.getWorld().getUID(), p.getWorld().getFullTime(),
                ctx().config().getLong("max_jump_ticks", 400)) * 50;
    }

    @Override
    protected boolean conditionHolds(Player p) {
        return p.getWorld().getEnvironment() == World.Environment.NORMAL
                && p.getEyeLocation().getBlock().getLightFromSky() == 0;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        clock.forget(e.getPlayer().getUniqueId());
    }
}

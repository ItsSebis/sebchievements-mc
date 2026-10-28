package dev.sebi.sebchievements.handlers.silly;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.handlers.support.TimedHandler;
import org.bukkit.entity.Player;

/** Survive on half a heart or less for the configured time. */
public final class ItsFineHandler extends TimedHandler {
    @Override
    public String id() {
        return AdvancementIds.ITS_FINE;
    }

    @Override
    protected boolean conditionHolds(Player p) {
        return !p.isDead() && p.getHealth() <= ctx().config().getDouble("max_health", 1.0);
    }
}

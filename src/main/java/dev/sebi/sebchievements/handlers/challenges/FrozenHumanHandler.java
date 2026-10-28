package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.handlers.support.TimedHandler;
import org.bukkit.entity.Player;

/** Fully frozen (freeze ticks at the maximum) continuously for the configured time; thawing or dying resets it. */
public final class FrozenHumanHandler extends TimedHandler {
    @Override
    public String id() {
        return AdvancementIds.FROZEN_HUMAN;
    }

    @Override
    protected boolean conditionHolds(Player p) {
        return !p.isDead() && p.isFrozen();
    }
}

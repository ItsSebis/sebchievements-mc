package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.handlers.support.TimedHandler;
import org.bukkit.entity.Player;

/** Playtime without dying. AFK, creative/spectator and logging out pause it; death resets it. Riding is allowed. */
public final class IronWillHandler extends TimedHandler {
    @Override
    public String id() {
        return AdvancementIds.IRON_WILL;
    }

    @Override
    protected boolean conditionHolds(Player p) {
        return !p.isDead();
    }

    @Override
    protected boolean ridingAllowed() {
        return true;
    }
}

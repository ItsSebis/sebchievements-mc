package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.handlers.support.BlockCountHandler;
import org.bukkit.event.block.BlockBreakEvent;

/** Any block that takes time to break; plants and torches that pop off instantly don't count. */
public final class MovingMountainsHandler extends BlockCountHandler {
    @Override
    public String id() {
        return AdvancementIds.MOVING_MOUNTAINS;
    }

    @Override
    protected long defaultTarget() {
        return 100_000;
    }

    @Override
    protected boolean counts(BlockBreakEvent e) {
        return e.getBlock().getType().getHardness() > 0;
    }
}

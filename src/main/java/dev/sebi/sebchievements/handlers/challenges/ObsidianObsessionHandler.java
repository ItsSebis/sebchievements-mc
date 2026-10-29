package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.handlers.support.BlockCountHandler;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;

/** Obsidian broken with a tool that is right for it (a pickaxe good enough to drop it). */
public final class ObsidianObsessionHandler extends BlockCountHandler {
    @Override
    public String id() {
        return AdvancementIds.OBSIDIAN_OBSESSION;
    }

    @Override
    protected long defaultTarget() {
        return 1000;
    }

    @Override
    protected boolean counts(BlockBreakEvent e) {
        return e.getBlock().getType() == Material.OBSIDIAN
                && e.getBlock().isPreferredTool(e.getPlayer().getInventory().getItemInMainHand());
    }
}

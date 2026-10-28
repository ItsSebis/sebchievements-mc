package dev.sebi.sebchievements.handlers.silly;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.handlers.support.TimedHandler;
import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Look at an Enderman for the configured time while wearing a carved pumpkin. */
public final class StaringContestHandler extends TimedHandler {
    @Override
    public String id() {
        return AdvancementIds.STARING_CONTEST;
    }

    @Override
    protected boolean conditionHolds(Player p) {
        ItemStack helmet = p.getInventory().getHelmet();
        if (helmet == null || helmet.getType() != Material.CARVED_PUMPKIN) return false;
        return p.getTargetEntity(ctx().config().getInt("range", 32)) instanceof Enderman;
    }
}

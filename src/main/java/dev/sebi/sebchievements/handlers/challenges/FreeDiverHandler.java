package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.handlers.support.TimedHandler;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

/**
 * Time with the eyes underwater and no breathing aid. By default cumulative (surfacing pauses the timer),
 * because a continuous 2 minutes without any aid means drowning after about 15 seconds; set
 * {@code reset_on_surface: true} for the strict version.
 */
public final class FreeDiverHandler extends TimedHandler {
    @Override
    public String id() {
        return AdvancementIds.FREE_DIVER;
    }

    @Override
    protected boolean pausesWhenConditionFails() {
        return !ctx().config().getBoolean("reset_on_surface", false);
    }

    @Override
    protected boolean conditionHolds(Player p) {
        if (p.isDead() || !p.isUnderWater()) return false;
        if (p.hasPotionEffect(PotionEffectType.WATER_BREATHING) || p.hasPotionEffect(PotionEffectType.CONDUIT_POWER)) return false;
        ItemStack helmet = p.getInventory().getHelmet();
        return helmet == null || !helmet.containsEnchantment(Enchantment.RESPIRATION);
    }
}

package dev.sebi.sebchievements.handlers.social;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** A splash potion of Healing or Regeneration hits another player whose health was below the threshold. */
public final class MedicHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.MEDIC;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSplash(PotionSplashEvent e) {
        ThrownPotion potion = e.getPotion();
        if (!(potion.getShooter() instanceof Player thrower)) return;
        boolean heals = false;
        for (PotionEffect effect : potion.getEffects()) {
            if (effect.getType().equals(PotionEffectType.INSTANT_HEALTH) || effect.getType().equals(PotionEffectType.REGENERATION)) heals = true;
        }
        if (!heals) return;
        double below = ctx.config().getDouble("max_health", 8.0);
        for (LivingEntity target : e.getAffectedEntities()) {
            // health is read here, before the effect is applied
            if (target instanceof Player hurt && !hurt.equals(thrower) && e.getIntensity(target) > 0 && hurt.getHealth() < below) {
                ctx.grant(thrower);
                return;
            }
        }
    }
}

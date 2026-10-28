package dev.sebi.sebchievements.handlers.social;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

/**
 * Be within the radius when another player completes an advancement. Recipes, tab roots, advancements
 * without a display, and Witness itself are ignored.
 */
public final class WitnessHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.WITNESS;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        Advancement adv = e.getAdvancement();
        if (adv.getDisplay() == null || adv.getParent() == null) return;
        if (adv.getKey().equals(AdvancementIds.key(ctx.id()))) return;
        Player earner = e.getPlayer();
        for (Player witness : ctx.services().proximity().nearby(earner, ctx.config().getDouble("radius", 16))) {
            ctx.grant(witness);
        }
    }
}

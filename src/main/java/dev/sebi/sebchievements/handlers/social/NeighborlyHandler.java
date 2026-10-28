package dev.sebi.sebchievements.handlers.social;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.GameMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.UUID;

/**
 * Picking up an item another player threw. Also records both players as having "traded", which Betrayal reads
 * (UUID set in the PDC under {@link #TRADED_KEY}).
 */
public final class NeighborlyHandler implements AdvancementHandler, Listener {
    /** Shared with {@link BetrayalHandler}. Ends in /neighborly so /adv debug and /adv reset see it. */
    public static final String TRADED_KEY = "traded/" + AdvancementIds.NEIGHBORLY;

    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.NEIGHBORLY;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player picker)) return;
        UUID thrower = e.getItem().getThrower();
        if (thrower == null || thrower.equals(picker.getUniqueId())) return;
        if (picker.getGameMode() == GameMode.SPECTATOR) return;
        ctx.store().addToUuidSet(picker, TRADED_KEY, thrower);
        Player other = Bukkit.getPlayer(thrower);
        if (other != null) ctx.store().addToUuidSet(other, TRADED_KEY, picker.getUniqueId());
        ctx.grant(picker);
    }
}

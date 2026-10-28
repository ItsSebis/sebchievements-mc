package dev.sebi.sebchievements.handlers.social;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/** Remembers the last player who killed you; killing them grants it (and clears the memory). */
public final class RevengeHandler implements AdvancementHandler, Listener {
    private static final String KEY = "lastkiller/" + AdvancementIds.REVENGE;

    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.REVENGE;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getPlayer();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;
        if (victim.getUniqueId().toString().equals(ctx.store().getString(killer, KEY, ""))) {
            ctx.grant(killer);
            ctx.store().remove(killer, KEY);
        }
        ctx.store().setString(victim, KEY, killer.getUniqueId().toString());
    }
}

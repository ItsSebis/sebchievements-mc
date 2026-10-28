package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.core.Progress;
import dev.sebi.sebchievements.core.ProgressReporting;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;
import java.util.Optional;

/** The respawn time is stored in the PDC; crafting (not trading or being handed one) a diamond pickaxe inside the window grants. */
public final class FreshStartHandler implements AdvancementHandler, ProgressReporting, Listener {
    private static final String KEY = "respawn/" + AdvancementIds.FRESH_START;

    private HandlerContext ctx;
    private long windowMillis;

    @Override
    public String id() {
        return AdvancementIds.FRESH_START;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.windowMillis = ctx.config().getLong("seconds", 600) * 1000;
        ctx.listen(this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        ctx.store().setLong(e.getPlayer(), KEY, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || e.getRecipe().getResult().getType() != Material.DIAMOND_PICKAXE) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        long respawned = ctx.store().getLong(p, KEY, -1);
        if (respawned >= 0 && System.currentTimeMillis() - respawned <= windowMillis) ctx.grant(p);
    }

    @Override
    public Optional<Progress> progress(Player p) {
        long respawned = ctx.store().getLong(p, KEY, -1);
        if (respawned < 0) return Optional.empty();
        return Optional.of(Progress.time(System.currentTimeMillis() - respawned, windowMillis));
    }

    @Override
    public List<String> debug(Player p) {
        return List.of("respawned at = " + ctx.store().getLong(p, KEY, -1));
    }
}

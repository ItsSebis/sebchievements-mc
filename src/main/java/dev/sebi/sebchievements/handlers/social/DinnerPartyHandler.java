package dev.sebi.sebchievements.handlers.social;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Enough players eat from the same cake within the window; all of them get it. Each cake block position is
 * a WindowService "player" (a name-based UUID of its location) and the eaters are the event keys.
 */
public final class DinnerPartyHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.DINNER_PARTY;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) {
        Block cake = e.getClickedBlock();
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || cake == null || cake.getType() != Material.CAKE) return;
        if (e.useInteractedBlock() == Event.Result.DENY || e.getHand() == null) return;
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.getFoodLevel() >= 20) return; // a full player cannot take a bite

        long now = System.currentTimeMillis();
        long window = ctx.config().getLong("seconds", 60) * 1000;
        int needed = ctx.config().getInt("players", 3);
        UUID cakeId = UUID.nameUUIDFromBytes(location(cake.getLocation()).getBytes(StandardCharsets.UTF_8));
        if (ctx.services().windows().record(cakeId, ctx.id(), p.getUniqueId().toString(), now, window) < needed) return;
        for (String eater : ctx.services().windows().keys(cakeId, ctx.id(), now, window)) {
            Player who = Bukkit.getPlayer(UUID.fromString(eater));
            if (who != null) ctx.grant(who);
        }
    }

    private static String location(Location l) {
        return l.getWorld().getUID() + ":" + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }
}

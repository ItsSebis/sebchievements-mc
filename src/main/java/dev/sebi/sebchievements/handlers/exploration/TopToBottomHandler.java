package dev.sebi.sebchievements.handlers.exploration;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Overworld only. Standing on the highest placeable block (feet at the world's max height) and on the lowest
 * bedrock (feet one above the min height) both count within the window; each is refreshed while the player is there.
 */
public final class TopToBottomHandler implements AdvancementHandler {
    @Override
    public String id() {
        return AdvancementIds.TOP_TO_BOTTOM;
    }

    @Override
    public void register(HandlerContext ctx) {
        long window = ctx.config().getLong("seconds", 300) * 1000;
        int margin = ctx.config().getInt("margin", 0);
        ctx.services().tick().register(ctx.id(), p -> {
            World w = p.getWorld();
            if (w.getEnvironment() != World.Environment.NORMAL
                    || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
            double y = p.getLocation().getY();
            String edge = y >= w.getMaxHeight() - margin ? "top" : y <= w.getMinHeight() + 1 + margin ? "bottom" : null;
            if (edge == null) return;
            if (ctx.services().windows().record(p.getUniqueId(), ctx.id(), edge, System.currentTimeMillis(), window) >= 2) {
                ctx.grant(p);
            }
        });
    }
}

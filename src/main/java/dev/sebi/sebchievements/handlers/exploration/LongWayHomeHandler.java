package dev.sebi.sebchievements.handlers.exploration;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Die in the Overworld at least N blocks (horizontal) from where you would respawn: your bed or anchor if it is
 * in the same world, otherwise the world spawn.
 */
public final class LongWayHomeHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;

    @Override
    public String id() {
        return AdvancementIds.LONG_WAY_HOME;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        ctx.listen(this);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        World world = p.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return;
        Location home = p.getRespawnLocation();
        if (home == null || !world.equals(home.getWorld())) home = world.getSpawnLocation();
        Location died = p.getLocation();
        double blocks = ctx.config().getDouble("blocks", 10_000);
        if (Math.hypot(died.getX() - home.getX(), died.getZ() - home.getZ()) >= blocks) ctx.grant(p);
    }
}

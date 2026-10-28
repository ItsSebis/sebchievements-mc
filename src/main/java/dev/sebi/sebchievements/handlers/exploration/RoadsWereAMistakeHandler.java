package dev.sebi.sebchievements.handlers.exploration;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.core.Progress;
import dev.sebi.sebchievements.core.ProgressReporting;
import dev.sebi.sebchievements.services.RideTracker;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Distance travelled on ice in one boat ride, sampled once per second (RideTracker). */
public final class RoadsWereAMistakeHandler implements AdvancementHandler, ProgressReporting, Listener {
    private static final Set<Material> ICE = Set.of(Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.FROSTED_ICE);

    private final RideTracker rides = new RideTracker();
    private HandlerContext ctx;
    private double target;
    private double maxStep;

    @Override
    public String id() {
        return AdvancementIds.ROADS_WERE_A_MISTAKE;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.target = ctx.config().getDouble("blocks", 1000);
        this.maxStep = ctx.config().getDouble("max_step_blocks", 150);
        ctx.services().tick().register(ctx.id(), this::check);
        ctx.listen(this);
    }

    private void check(Player p) {
        Entity vehicle = p.getVehicle();
        if (!(vehicle instanceof Boat) || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
            rides.dismount(p.getUniqueId());
            return;
        }
        Location at = vehicle.getLocation();
        boolean onIce = ICE.contains(at.clone().subtract(0, 0.5, 0).getBlock().getType());
        double meters = rides.update(p.getUniqueId(), vehicle.getUniqueId(), at.getX(), at.getZ(), onIce, maxStep);
        if (meters >= target) {
            ctx.grant(p);
            rides.dismount(p.getUniqueId());
        }
    }

    @EventHandler
    public void onExit(VehicleExitEvent e) {
        if (e.getExited() instanceof Player p) rides.dismount(p.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        rides.dismount(e.getPlayer().getUniqueId());
    }

    @Override
    public Optional<Progress> progress(Player player) {
        return Optional.of(Progress.count((long) rides.distance(player.getUniqueId()), (long) target));
    }

    @Override
    public List<String> debug(Player player) {
        return List.of("ride distance = " + (long) rides.distance(player.getUniqueId()) + " / " + (long) target);
    }
}

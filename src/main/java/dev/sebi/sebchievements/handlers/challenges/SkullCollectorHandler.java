package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.core.Progress;
import dev.sebi.sebchievements.core.ProgressReporting;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Wither skeleton skulls dropped by wither skeletons a survival or adventure player killed. The counter starts at the
 * game's own "picked up" statistic for the skull (capped at the wither skeletons killed), read once when the plugin first sees the player, so earlier work
 * counts. After that only drops count, so dropping and re-picking a skull gains nothing.
 */
public final class SkullCollectorHandler implements AdvancementHandler, ProgressReporting, Listener {
    private static final String SEEDED_KEY = "statseed/" + AdvancementIds.SKULL_COLLECTOR;

    private HandlerContext ctx;
    private long target;

    @Override
    public String id() {
        return AdvancementIds.SKULL_COLLECTOR;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.target = ctx.config().getLong("skulls", 50);
        ctx.services().tick().register(ctx.id(), this::seed);
        ctx.listen(this);
    }

    private void seed(Player p) {
        if (ctx.store().getLong(p, SEEDED_KEY, 0) != 0) return;
        ctx.store().setLong(p, SEEDED_KEY, 1);
        long before = earlierSkulls(p);
        if (ctx.services().counters().add(p.getUniqueId(), ctx.id(), before) >= target) ctx.grant(p);
    }

    /** Skulls picked up so far, but never more than wither skeletons killed (a kill drops at most one skull). */
    private static long earlierSkulls(Player p) {
        return Math.min(p.getStatistic(Statistic.PICKUP, Material.WITHER_SKELETON_SKULL),
                p.getStatistic(Statistic.KILL_ENTITY, EntityType.WITHER_SKELETON));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        if (e.getEntityType() != EntityType.WITHER_SKELETON) return;
        Player p = e.getEntity().getKiller();
        if (p == null || (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE)) return;
        // a player the plugin hasn't seeded yet would have the stat added on top of this kill, so seed first
        seed(p);
        long skulls = 0;
        for (ItemStack s : e.getDrops()) if (s.getType() == Material.WITHER_SKELETON_SKULL) skulls += s.getAmount();
        if (skulls == 0) return;
        if (ctx.services().counters().add(p.getUniqueId(), ctx.id(), skulls) >= target) ctx.grant(p);
    }

    @Override
    public Optional<Progress> progress(Player player) {
        return Optional.of(Progress.count(ctx.services().counters().get(player.getUniqueId(), ctx.id()), target));
    }

    @Override
    public List<String> debug(Player player) {
        return List.of("skulls = " + ctx.services().counters().get(player.getUniqueId(), ctx.id()) + " / " + target,
                "seeded = " + (ctx.store().getLong(player, SEEDED_KEY, 0) != 0),
                "picked up stat = " + player.getStatistic(Statistic.PICKUP, Material.WITHER_SKELETON_SKULL),
                "wither skeletons killed stat = " + player.getStatistic(Statistic.KILL_ENTITY, EntityType.WITHER_SKELETON),
                "seed value = " + earlierSkulls(player));
    }
}

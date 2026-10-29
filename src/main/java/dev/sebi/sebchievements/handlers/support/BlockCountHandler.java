package dev.sebi.sebchievements.handlers.support;

import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.core.Progress;
import dev.sebi.sebchievements.core.ProgressReporting;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.List;
import java.util.Optional;

/**
 * A counter of blocks mined by a survival player. Blocks a player placed in the last
 * {@code placed_block_ignore_minutes} don't count, so place-and-break loops are worthless.
 */
public abstract class BlockCountHandler implements AdvancementHandler, ProgressReporting, Listener {
    protected HandlerContext ctx;
    private long target;

    /** Is this broken block one the advancement is about? */
    protected abstract boolean counts(BlockBreakEvent e);

    protected abstract long defaultTarget();

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.target = ctx.config().getLong("blocks", defaultTarget());
        ctx.listen(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) return;
        Block b = e.getBlock();
        if (ctx.services().placed().recentlyPlaced(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ(), System.currentTimeMillis())) return;
        if (!counts(e)) return;
        if (ctx.services().counters().increment(p.getUniqueId(), ctx.id()) >= target) ctx.grant(p);
    }

    @Override
    public Optional<Progress> progress(Player player) {
        return Optional.of(Progress.count(ctx.services().counters().get(player.getUniqueId(), ctx.id()), target));
    }

    @Override
    public List<String> debug(Player player) {
        return List.of("blocks = " + ctx.services().counters().get(player.getUniqueId(), ctx.id()) + " / " + target);
    }
}

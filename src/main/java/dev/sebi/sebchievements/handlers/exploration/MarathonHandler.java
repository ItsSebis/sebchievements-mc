package dev.sebi.sebchievements.handlers.exploration;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.core.Progress;
import dev.sebi.sebchievements.core.ProgressReporting;
import org.bukkit.GameMode;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Walking and sprinting distance, taken from the game's own statistics once per second. Only the growth since the
 * plugin first saw the player counts, and nothing is added in creative or spectator mode.
 */
public final class MarathonHandler implements AdvancementHandler, ProgressReporting {
    private static final String BASE_KEY = "statbase/" + AdvancementIds.MARATHON;

    private HandlerContext ctx;
    private long target;

    @Override
    public String id() {
        return AdvancementIds.MARATHON;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.target = ctx.config().getLong("blocks", 100_000);
        ctx.services().tick().register(ctx.id(), this::check);
    }

    private static long footCm(Player p) {
        return (long) p.getStatistic(Statistic.WALK_ONE_CM) + p.getStatistic(Statistic.SPRINT_ONE_CM);
    }

    private void check(Player p) {
        long now = footCm(p);
        long base = ctx.store().getLong(p, BASE_KEY, -1);
        if (base < 0 || now < base) {
            ctx.store().setLong(p, BASE_KEY, now);
            return;
        }
        long metres = (now - base) / 100;
        if (metres == 0) return;
        // advance the baseline by whole metres only, so the centimetres left over are counted next time
        ctx.store().setLong(p, BASE_KEY, base + metres * 100);
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (ctx.services().counters().add(p.getUniqueId(), ctx.id(), metres) >= target) ctx.grant(p);
    }

    @Override
    public Optional<Progress> progress(Player player) {
        return Optional.of(Progress.count(ctx.services().counters().get(player.getUniqueId(), ctx.id()), target));
    }

    @Override
    public List<String> debug(Player player) {
        return List.of("blocks on foot = " + ctx.services().counters().get(player.getUniqueId(), ctx.id()) + " / " + target);
    }
}

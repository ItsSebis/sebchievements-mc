package dev.sebi.sebchievements.handlers.silly;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.core.Progress;
import dev.sebi.sebchievements.core.ProgressReporting;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;

import java.util.List;
import java.util.Optional;

/** Counts every night skipped by sleeping for each player who was sleeping at that moment. */
public final class ProfessionalNapperHandler implements AdvancementHandler, ProgressReporting, Listener {
    private HandlerContext ctx;
    private long target;

    @Override
    public String id() {
        return AdvancementIds.PROFESSIONAL_NAPPER;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.target = ctx.config().getLong("nights", 100);
        ctx.listen(this);
    }

    @EventHandler
    public void onTimeSkip(TimeSkipEvent e) {
        if (e.getSkipReason() != TimeSkipEvent.SkipReason.NIGHT_SKIP || e.getWorld().getEnvironment() != World.Environment.NORMAL) return;
        for (Player p : e.getWorld().getPlayers()) {
            if (!p.isSleeping() || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            long nights = ctx.services().counters().increment(p.getUniqueId(), ctx.id());
            if (nights >= target) ctx.grant(p);
        }
    }

    @Override
    public Optional<Progress> progress(Player player) {
        return Optional.of(Progress.count(ctx.services().counters().get(player.getUniqueId(), ctx.id()), target));
    }

    @Override
    public List<String> debug(Player player) {
        return List.of("nights = " + ctx.services().counters().get(player.getUniqueId(), ctx.id()) + " / " + target);
    }
}

package dev.sebi.sebchievements.handlers.support;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.core.Progress;
import dev.sebi.sebchievements.core.ProgressReporting;
import dev.sebi.sebchievements.services.TickScheduler;
import dev.sebi.sebchievements.services.TimerService.State;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;
import java.util.Optional;

/**
 * Base for "condition held continuously for N seconds" advancements. Applies the global anti-cheese policy
 * (CLAUDE.md section 5): time pauses while AFK, in creative/spectator, or riding (unless {@link #ridingAllowed});
 * the timer resets on death; logging out pauses because only online players are ticked.
 */
public abstract class TimedHandler implements AdvancementHandler, ProgressReporting, Listener {
    private HandlerContext ctx;
    private Advancement advancement;
    private long requiredMillis;

    /** The advancement-specific condition, evaluated once per second for players not excluded by policy. */
    protected abstract boolean conditionHolds(Player player);

    protected boolean ridingAllowed() {
        return false;
    }

    protected boolean resetOnDeath() {
        return true;
    }

    protected HandlerContext ctx() {
        return ctx;
    }

    /** Pure decision so the policy is testable: excluded players pause, others hold or break. */
    public static State decide(boolean excluded, boolean conditionHolds) {
        if (excluded) return State.PAUSED;
        return conditionHolds ? State.HELD : State.BROKEN;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.advancement = Bukkit.getAdvancement(AdvancementIds.key(ctx.id()));
        this.requiredMillis = ctx.config().getLong("seconds") * 1000;
        ctx.services().tick().register(ctx.id(), this::check);
        ctx.listen(this);
    }

    private void check(Player p) {
        if (advancement != null && p.getAdvancementProgress(advancement).isDone()) return;
        boolean excluded = p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR
                || ctx.services().afk().isAfk(p) || (!ridingAllowed() && p.isInsideVehicle());
        State state = decide(excluded, !excluded && conditionHolds(p));
        long elapsed = ctx.services().timers().tick(p.getUniqueId(), ctx.id(), state, TickScheduler.INTERVAL_MILLIS);
        if (elapsed >= requiredMillis) {
            ctx.grant(p);
            ctx.services().timers().reset(p.getUniqueId(), ctx.id());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (resetOnDeath()) ctx.services().timers().reset(e.getPlayer().getUniqueId(), ctx.id());
    }

    @Override
    public Optional<Progress> progress(Player player) {
        return Optional.of(Progress.time(ctx.services().timers().elapsed(player.getUniqueId(), ctx.id()), requiredMillis));
    }

    @Override
    public List<String> debug(Player player) {
        return List.of("elapsed ms = " + ctx.services().timers().elapsed(player.getUniqueId(), ctx.id())
                + " / " + requiredMillis);
    }
}

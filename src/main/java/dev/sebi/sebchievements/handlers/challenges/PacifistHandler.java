package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.core.Progress;
import dev.sebi.sebchievements.core.ProgressReporting;
import dev.sebi.sebchievements.services.NightTracker;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.TimeSkipEvent;

import java.util.List;
import java.util.Optional;

/**
 * Consecutive clean nights. The Overworld clock is used for everyone. Missing a night (offline, partial, skipped,
 * AFK, in another dimension) or killing any mob resets the streak; consecutive nights are detected from the day
 * number of the last counted night, so nights spent offline break it too.
 */
public final class PacifistHandler implements AdvancementHandler, ProgressReporting, Listener {
    private static final long TICKS_PER_DAY = 24000;
    private static final String LAST_NIGHT_KEY = "lastnight/" + AdvancementIds.PACIFIST;

    private final NightTracker nights = new NightTracker();
    private HandlerContext ctx;
    private long target, start, end, slack;

    @Override
    public String id() {
        return AdvancementIds.PACIFIST;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.target = ctx.config().getLong("nights", 3);
        this.start = ctx.config().getLong("night_start", 13000);
        this.end = ctx.config().getLong("night_end", 23000);
        this.slack = ctx.config().getLong("slack_ticks", 40);
        ctx.services().tick().register(ctx.id(), this::check);
        ctx.listen(this);
    }

    private void check(Player p) {
        World overworld = Bukkit.getWorlds().get(0);
        long tod = overworld.getTime();
        long day = (overworld.getFullTime() - tod) / TICKS_PER_DAY;
        boolean clean = p.getWorld().equals(overworld) && !ctx.services().afk().isAfk(p)
                && p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR;
        apply(p, nights.sample(p.getUniqueId(), day, tod, clean, start, end, slack));
    }

    private void apply(Player p, NightTracker.Outcome outcome) {
        switch (outcome.result()) {
            case FAILED -> ctx.services().streaks().reset(p.getUniqueId(), ctx.id());
            case COMPLETED -> {
                long last = ctx.store().getLong(p, LAST_NIGHT_KEY, Long.MIN_VALUE);
                if (last != outcome.day() - 1) ctx.services().streaks().reset(p.getUniqueId(), ctx.id());
                ctx.store().setLong(p, LAST_NIGHT_KEY, outcome.day());
                if (ctx.services().streaks().increment(p.getUniqueId(), ctx.id()) >= target) {
                    ctx.grant(p);
                    ctx.services().streaks().reset(p.getUniqueId(), ctx.id());
                }
            }
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null || !(e.getEntity() instanceof Mob)) return;
        ctx.services().streaks().reset(killer.getUniqueId(), ctx.id());
        nights.disqualify(killer.getUniqueId());
    }

    @EventHandler
    public void onTimeSkip(TimeSkipEvent e) {
        // a skipped night is a failed night for everyone who was part of it
        for (Player p : Bukkit.getOnlinePlayers()) nights.disqualify(p.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        apply(e.getPlayer(), nights.leave(e.getPlayer().getUniqueId()));
    }

    @Override
    public Optional<Progress> progress(Player player) {
        return Optional.of(Progress.count(ctx.services().streaks().get(player.getUniqueId(), ctx.id()), target));
    }

    @Override
    public List<String> debug(Player player) {
        return List.of("streak = " + ctx.services().streaks().get(player.getUniqueId(), ctx.id()) + " / " + target,
                "last counted night (day number) = " + ctx.store().getLong(player, LAST_NIGHT_KEY, -1));
    }
}

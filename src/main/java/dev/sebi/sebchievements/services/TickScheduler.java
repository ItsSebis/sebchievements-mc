package dev.sebi.sebchievements.services;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * The one repeating task (every {@link #INTERVAL_TICKS} ticks) that runs every registered per-player check
 * for every online player. Checks run in registration order, so AFK sampling goes first.
 */
public final class TickScheduler {
    public static final int INTERVAL_TICKS = 20;
    public static final long INTERVAL_MILLIS = INTERVAL_TICKS * 50L;

    private record Check(String name, Consumer<Player> action) {}

    private final Plugin plugin;
    private final List<Check> checks = new ArrayList<>();
    private final List<Runnable> globals = new ArrayList<>();
    private BukkitTask task;

    public TickScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(String name, Consumer<Player> check) {
        checks.add(new Check(name, check));
    }

    /** Runs once per interval, not per player (housekeeping). */
    public void registerGlobal(Runnable action) {
        globals.add(action);
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::run, INTERVAL_TICKS, INTERVAL_TICKS);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void run() {
        for (Runnable g : globals) {
            try {
                g.run();
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Global tick task failed", e);
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (Check c : checks) {
                try {
                    c.action().accept(p);
                } catch (RuntimeException e) {
                    plugin.getLogger().log(Level.WARNING, "Check '" + c.name() + "' failed for " + p.getName(), e);
                }
            }
        }
    }
}

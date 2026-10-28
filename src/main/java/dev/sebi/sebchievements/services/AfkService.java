package dev.sebi.sebchievements.services;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.function.LongSupplier;

/** Bukkit side of {@link AfkTracker}. Register {@link #sample} on the TickScheduler before any other check. */
public final class AfkService implements Listener {
    private final AfkTracker tracker = new AfkTracker();
    private final LongSupplier afkMillis;

    /** @param afkMillis supplier so a config reload takes effect */
    public AfkService(LongSupplier afkMillis) {
        this.afkMillis = afkMillis;
    }

    public void sample(Player p) {
        Location l = p.getLocation();
        tracker.update(p.getUniqueId(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), System.currentTimeMillis());
    }

    public boolean isAfk(Player p) {
        return tracker.isAfk(p.getUniqueId(), System.currentTimeMillis(), afkMillis.getAsLong());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        tracker.remove(e.getPlayer().getUniqueId());
    }
}

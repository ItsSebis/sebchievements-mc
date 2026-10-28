package dev.sebi.sebchievements.services;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.List;

/** Nearby players within a radius, spectators excluded. */
public final class ProximityService {
    /** Other players in the same world within {@code radius} blocks of {@code center}. */
    public List<Player> nearby(Player center, double radius) {
        double r2 = radius * radius;
        return center.getWorld().getPlayers().stream()
                .filter(p -> !p.equals(center))
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .filter(p -> p.getLocation().distanceSquared(center.getLocation()) <= r2)
                .toList();
    }
}

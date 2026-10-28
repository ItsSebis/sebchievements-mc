package dev.sebi.sebchievements.core;

import org.bukkit.entity.Player;

import java.util.Optional;

/** Implemented by handlers of timed/streak advancements so {@code /adv progress} can list them. */
public interface ProgressReporting {
    Optional<Progress> progress(Player player);
}

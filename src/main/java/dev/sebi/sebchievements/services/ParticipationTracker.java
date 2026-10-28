package dev.sebi.sebchievements.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Who hurt a given victim and whether each of them stayed qualified. One bad hit disqualifies a player for that
 * victim for good (armor on, a projectile used...), which is how the per-boss challenges are judged.
 */
public final class ParticipationTracker {
    private final Map<UUID, Map<UUID, Boolean>> victims = new HashMap<>();

    public void hit(UUID victim, UUID player, boolean qualifies) {
        victims.computeIfAbsent(victim, v -> new HashMap<>()).merge(player, qualifies, Boolean::logicalAnd);
    }

    /** Players who hurt the victim and never disqualified themselves. */
    public Set<UUID> qualified(UUID victim) {
        Set<UUID> out = new HashSet<>();
        victims.getOrDefault(victim, Map.of()).forEach((p, ok) -> {
            if (ok) out.add(p);
        });
        return out;
    }

    public void forget(UUID victim) {
        victims.remove(victim);
    }
}

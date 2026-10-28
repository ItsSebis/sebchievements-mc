package dev.sebi.sebchievements.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Game ticks elapsed between a player's consecutive checks. Unlike wall-clock time this follows lag, and it
 * ignores time jumps (sleeping through a night, /time set) and world changes so they can't be farmed.
 */
public final class GameClock {
    private record Sample(UUID world, long fullTime) {}

    private final Map<UUID, Sample> last = new HashMap<>();

    /** @return ticks since this player's previous call in the same world, or 0 on the first call, a world change, a backwards step, or a jump above {@code maxJump} */
    public long deltaTicks(UUID player, UUID world, long fullTime, long maxJump) {
        Sample prev = last.put(player, new Sample(world, fullTime));
        if (prev == null || !prev.world().equals(world)) return 0;
        long d = fullTime - prev.fullTime();
        return d < 0 || d > maxJump ? 0 : d;
    }

    public void forget(UUID player) {
        last.remove(player);
    }
}

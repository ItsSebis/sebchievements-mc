package dev.sebi.sebchievements.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Blocks a player placed recently, so that breaking them again doesn't count as mining (place, break, repeat).
 * Memory only and time-limited on purpose: remembering every placed block forever would bloat the world data.
 */
public final class PlacedBlocks {
    private record Pos(UUID world, int x, int y, int z) {}

    private final long ignoreMillis;
    private final Map<Pos, Long> placedAt = new HashMap<>();

    public PlacedBlocks(long ignoreMillis) {
        this.ignoreMillis = ignoreMillis;
    }

    public void placed(UUID world, int x, int y, int z, long now) {
        placedAt.put(new Pos(world, x, y, z), now);
    }

    public boolean recentlyPlaced(UUID world, int x, int y, int z, long now) {
        Long at = placedAt.get(new Pos(world, x, y, z));
        return at != null && now - at < ignoreMillis;
    }

    public void purge(long now) {
        placedAt.values().removeIf(at -> now - at >= ignoreMillis);
    }

    public int size() {
        return placedAt.size();
    }
}

package dev.sebi.sebchievements.services;

import java.util.UUID;

/** Counted streaks that reset on failure. */
public final class StreakService {
    private final LongStore store;

    public StreakService(LongStore store) {
        this.store = store;
    }

    private static String key(String id) {
        return "streak/" + id;
    }

    /** @return the new streak length */
    public long increment(UUID player, String id) {
        long v = get(player, id) + 1;
        store.set(player, key(id), v);
        return v;
    }

    public long get(UUID player, String id) {
        return store.get(player, key(id), 0);
    }

    public void reset(UUID player, String id) {
        store.remove(player, key(id));
    }
}

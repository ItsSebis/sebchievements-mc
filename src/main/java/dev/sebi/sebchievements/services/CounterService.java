package dev.sebi.sebchievements.services;

import java.util.UUID;

/** Simple persistent counters that never reset on their own. */
public final class CounterService {
    private final LongStore store;

    public CounterService(LongStore store) {
        this.store = store;
    }

    private static String key(String id) {
        return "counter/" + id;
    }

    public long increment(UUID player, String id) {
        return add(player, id, 1);
    }

    public long add(UUID player, String id, long delta) {
        long v = get(player, id) + delta;
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

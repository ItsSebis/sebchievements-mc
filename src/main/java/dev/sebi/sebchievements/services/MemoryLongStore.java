package dev.sebi.sebchievements.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MemoryLongStore implements LongStore {
    private final Map<String, Long> values = new HashMap<>();

    private static String k(UUID p, String key) {
        return p + "|" + key;
    }

    @Override
    public long get(UUID player, String key, long def) {
        return values.getOrDefault(k(player, key), def);
    }

    @Override
    public void set(UUID player, String key, long value) {
        values.put(k(player, key), value);
    }

    @Override
    public void remove(UUID player, String key) {
        values.remove(k(player, key));
    }
}

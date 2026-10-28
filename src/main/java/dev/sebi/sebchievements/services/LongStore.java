package dev.sebi.sebchievements.services;

import java.util.UUID;

/** Per-player long values. Production: PersistentDataContainer; tests: in memory. */
public interface LongStore {
    long get(UUID player, String key, long def);

    void set(UUID player, String key, long value);

    void remove(UUID player, String key);
}

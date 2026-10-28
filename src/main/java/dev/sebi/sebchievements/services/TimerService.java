package dev.sebi.sebchievements.services;

import java.util.UUID;

/**
 * "Condition held continuously for N seconds", per player and advancement id.
 * Elapsed time is persisted, so logging out pauses (simply: nobody ticks an offline player).
 */
public final class TimerService {
    /** What the condition did since the last tick. */
    public enum State {
        /** Condition holds: time accumulates. */
        HELD,
        /** Condition broken: the timer resets. */
        BROKEN,
        /** Neither (AFK, offline...): the timer keeps its value. */
        PAUSED
    }

    private final LongStore store;

    public TimerService(LongStore store) {
        this.store = store;
    }

    private static String key(String id) {
        return "timer/" + id;
    }

    /** @return elapsed millis after applying this tick */
    public long tick(UUID player, String id, State state, long deltaMillis) {
        switch (state) {
            case HELD -> {
                long v = elapsed(player, id) + deltaMillis;
                store.set(player, key(id), v);
                return v;
            }
            case BROKEN -> {
                reset(player, id);
                return 0;
            }
            default -> {
                return elapsed(player, id);
            }
        }
    }

    public long elapsed(UUID player, String id) {
        return store.get(player, key(id), 0);
    }

    public boolean reached(UUID player, String id, long requiredMillis) {
        return elapsed(player, id) >= requiredMillis;
    }

    public void reset(UUID player, String id) {
        store.remove(player, key(id));
    }
}

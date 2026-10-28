package dev.sebi.sebchievements.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class StreakAndCounterTest {
    private final UUID p = UUID.randomUUID();
    private final LongStore store = new MemoryLongStore();

    @Test
    void streakCountsAndResets() {
        StreakService s = new StreakService(store);
        s.increment(p, "x");
        assertEquals(2, s.increment(p, "x"));
        s.reset(p, "x");
        assertEquals(0, s.get(p, "x"));
        assertEquals(1, s.increment(p, "x"));
    }

    @Test
    void streakAndCounterDoNotShareState() {
        StreakService s = new StreakService(store);
        CounterService c = new CounterService(store);
        s.increment(p, "x");
        c.add(p, "x", 10);
        s.reset(p, "x");
        assertEquals(10, c.get(p, "x"));
    }
}

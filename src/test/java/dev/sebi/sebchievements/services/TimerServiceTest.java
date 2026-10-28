package dev.sebi.sebchievements.services;

import static dev.sebi.sebchievements.services.TimerService.State.BROKEN;
import static dev.sebi.sebchievements.services.TimerService.State.HELD;
import static dev.sebi.sebchievements.services.TimerService.State.PAUSED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimerServiceTest {
    private final UUID p = UUID.randomUUID();
    private final TimerService timers = new TimerService(new MemoryLongStore());

    @Test
    void accumulatesWhileHeld() {
        timers.tick(p, "a", HELD, 1000);
        assertEquals(3000, timers.tick(p, "a", HELD, 2000));
    }

    @Test
    void breakResets() {
        timers.tick(p, "a", HELD, 5000);
        assertEquals(0, timers.tick(p, "a", BROKEN, 1000));
        assertEquals(0, timers.elapsed(p, "a"));
    }

    @Test
    void pauseKeepsValue() {
        timers.tick(p, "a", HELD, 5000);
        assertEquals(5000, timers.tick(p, "a", PAUSED, 1000));
        assertEquals(6000, timers.tick(p, "a", HELD, 1000));
    }

    @Test
    void reachedAtThreshold() {
        timers.tick(p, "a", HELD, 999);
        assertFalse(timers.reached(p, "a", 1000));
        timers.tick(p, "a", HELD, 1);
        assertTrue(timers.reached(p, "a", 1000));
    }

    @Test
    void timersAreIndependentPerIdAndPlayer() {
        UUID other = UUID.randomUUID();
        timers.tick(p, "a", HELD, 1000);
        assertEquals(0, timers.elapsed(p, "b"));
        assertEquals(0, timers.elapsed(other, "a"));
    }
}

package dev.sebi.sebchievements.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AfkTrackerTest {
    private final UUID p = UUID.randomUUID();
    private final AfkTracker t = new AfkTracker();

    @Test
    void standingStillBecomesAfk() {
        t.update(p, 0, 64, 0, 0, 0, 0);
        t.update(p, 0, 64, 0, 0, 0, 100_000);
        assertFalse(t.isAfk(p, 100_000, 120_000));
        assertTrue(t.isAfk(p, 120_000, 120_000));
    }

    @Test
    void jitterDoesNotCountAsActivity() {
        t.update(p, 0, 64, 0, 0, 0, 0);
        t.update(p, 0.2, 64, 0.1, 2f, 1f, 100_000);
        assertTrue(t.isAfk(p, 120_000, 120_000));
    }

    @Test
    void realMovementResetsTheClock() {
        t.update(p, 0, 64, 0, 0, 0, 0);
        t.update(p, 3, 64, 0, 0, 0, 100_000);
        assertFalse(t.isAfk(p, 200_000, 120_000));
    }

    @Test
    void turningResetsTheClockIncludingAcrossTheYawWrap() {
        t.update(p, 0, 64, 0, 179f, 0, 0);
        t.update(p, 0, 64, 0, -179f, 0, 50_000); // only 2 degrees apart: not a turn
        assertTrue(t.isAfk(p, 120_000, 120_000));
        t.update(p, 0, 64, 0, 90f, 0, 100_000);
        assertFalse(t.isAfk(p, 120_000, 120_000));
    }

    @Test
    void unknownPlayerIsNotAfk() {
        assertFalse(t.isAfk(p, 1_000_000, 1000));
    }
}

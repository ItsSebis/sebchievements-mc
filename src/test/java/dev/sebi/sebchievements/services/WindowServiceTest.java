package dev.sebi.sebchievements.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class WindowServiceTest {
    private final UUID p = UUID.randomUUID();
    private final WindowService w = new WindowService();

    @Test
    void countsDistinctEventsInsideWindow() {
        assertEquals(1, w.record(p, "food", "rotten_flesh", 0, 60_000));
        assertEquals(2, w.record(p, "food", "spider_eye", 10_000, 60_000));
        assertEquals(2, w.record(p, "food", "spider_eye", 20_000, 60_000), "repeats are not distinct");
    }

    @Test
    void oldEventsFallOut() {
        w.record(p, "food", "a", 0, 60_000);
        w.record(p, "food", "b", 30_000, 60_000);
        assertEquals(2, w.record(p, "food", "c", 70_000, 60_000), "a (t=0) fell out, b and c remain");
        assertEquals(0, w.count(p, "food", 200_000, 60_000));
    }

    @Test
    void eventExactlyOnWindowEdgeStillCounts() {
        w.record(p, "x", "a", 0, 1000);
        assertEquals(2, w.record(p, "x", "b", 1000, 1000));
    }

    @Test
    void keysListsWhoIsInsideTheWindow() {
        w.record(p, "cake", "alice", 0, 60_000);
        w.record(p, "cake", "bob", 30_000, 60_000);
        assertEquals(java.util.Set.of("alice", "bob"), w.keys(p, "cake", 40_000, 60_000));
        assertEquals(java.util.Set.of("bob"), w.keys(p, "cake", 70_000, 60_000));
    }

    @Test
    void clearForgetsEverything() {
        w.record(p, "x", "a", 0, 1000);
        w.clear(p, "x");
        assertEquals(0, w.count(p, "x", 1, 1000));
    }
}

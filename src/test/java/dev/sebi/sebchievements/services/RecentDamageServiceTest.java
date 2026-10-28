package dev.sebi.sebchievements.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecentDamageServiceTest {
    private final UUID victim = UUID.randomUUID(), zombie = UUID.randomUUID(), skeleton = UUID.randomUUID();
    private final RecentDamageService s = new RecentDamageService(60_000);

    @Test
    void reportsOnlyHitsInsideTheWindow() {
        s.record(victim, zombie, 3, 0);
        s.record(victim, skeleton, 2, 4000);
        assertTrue(s.wasDamagedBy(victim, zombie, 5000, 5000));
        assertFalse(s.wasDamagedBy(victim, zombie, 5000, 5001));
        assertEquals(1, s.recent(victim, 5000, 5001).size());
    }

    @Test
    void victimsOfDamager() {
        s.record(victim, zombie, 3, 1000);
        assertEquals(List.of(victim), s.victimsOf(zombie, 5000, 2000));
        assertEquals(List.of(), s.victimsOf(skeleton, 5000, 2000));
    }

    @Test
    void purgeDropsOldEntries() {
        s.record(victim, zombie, 3, 0);
        s.purge(60_001);
        assertEquals(List.of(), s.recent(victim, Long.MAX_VALUE / 2, 60_001));
    }

    @Test
    void perVictimHistoryIsBounded() {
        for (int i = 0; i < 100; i++) s.record(victim, zombie, 1, i);
        assertEquals(32, s.recent(victim, 1000, 100).size());
    }
}

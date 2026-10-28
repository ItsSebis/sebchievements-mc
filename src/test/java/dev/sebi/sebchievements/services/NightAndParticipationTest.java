package dev.sebi.sebchievements.services;

import static dev.sebi.sebchievements.services.NightTracker.Result.COMPLETED;
import static dev.sebi.sebchievements.services.NightTracker.Result.FAILED;
import static dev.sebi.sebchievements.services.NightTracker.Result.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NightAndParticipationTest {
    private static final long START = 13000, END = 23000, SLACK = 40;
    private final UUID p = UUID.randomUUID(), victim = UUID.randomUUID(), other = UUID.randomUUID();
    private final NightTracker nights = new NightTracker();

    private NightTracker.Result sample(long tod, boolean clean) {
        return nights.sample(p, 5, tod, clean, START, END, SLACK).result();
    }

    @Test
    void cleanWholeNightCompletesAtDawn() {
        assertEquals(NONE, sample(13000, true));
        assertEquals(NONE, sample(18000, true));
        assertEquals(COMPLETED, nights.sample(p, 5, 23020, true, START, END, SLACK).result());
        assertEquals(NONE, sample(23040, true), "reported only once");
    }

    @Test
    void unCleanSampleFailsTheNight() {
        sample(13000, true);
        sample(15000, false);
        assertEquals(FAILED, nights.sample(p, 5, 23000, true, START, END, SLACK).result());
    }

    @Test
    void joiningMidNightIsAPartialNight() {
        sample(17000, true);
        assertEquals(FAILED, nights.sample(p, 5, 23000, true, START, END, SLACK).result());
    }

    @Test
    void joiningWithinSlackOfDuskStillCounts() {
        sample(13030, true);
        assertEquals(COMPLETED, nights.sample(p, 5, 23000, true, START, END, SLACK).result());
    }

    @Test
    void disqualifyAndLeaveFailTheNight() {
        sample(13000, true);
        nights.disqualify(p);
        assertEquals(FAILED, nights.sample(p, 5, 23000, true, START, END, SLACK).result());
        sample(13000, true);
        assertEquals(FAILED, nights.leave(p).result());
        assertEquals(NONE, nights.leave(p).result());
    }

    @Test
    void daytimeWithoutANightIsNothing() {
        assertEquals(NONE, sample(6000, true));
    }

    @Test
    void oneBadHitDisqualifiesForThatVictimOnly() {
        ParticipationTracker t = new ParticipationTracker();
        t.hit(victim, p, true);
        t.hit(victim, p, true);
        t.hit(victim, other, true);
        t.hit(victim, other, false);
        t.hit(victim, other, true);
        assertEquals(Set.of(p), t.qualified(victim));
        t.hit(UUID.randomUUID(), other, true);
        t.forget(victim);
        assertEquals(Set.of(), t.qualified(victim));
    }
}

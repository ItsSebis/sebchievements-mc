package dev.sebi.sebchievements.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class GameClockAndRideTest {
    private final UUID p = UUID.randomUUID(), w = UUID.randomUUID(), boat = UUID.randomUUID();

    @Test
    void clockCountsTicksBetweenChecksAndStartsAtZero() {
        GameClock c = new GameClock();
        assertEquals(0, c.deltaTicks(p, w, 1000, 400));
        assertEquals(20, c.deltaTicks(p, w, 1020, 400));
        assertEquals(35, c.deltaTicks(p, w, 1055, 400));
    }

    @Test
    void clockIgnoresJumpsBackwardsStepsAndWorldChanges() {
        GameClock c = new GameClock();
        c.deltaTicks(p, w, 1000, 400);
        assertEquals(0, c.deltaTicks(p, w, 13000, 400), "slept through the night");
        assertEquals(0, c.deltaTicks(p, w, 500, 400), "/time set backwards");
        assertEquals(0, c.deltaTicks(p, UUID.randomUUID(), 520, 400), "different world");
    }

    @Test
    void rideAddsDistanceOnlyBetweenCountingSamples() {
        RideTracker r = new RideTracker();
        assertEquals(0, r.update(p, boat, 0, 0, true, 150));
        assertEquals(30, r.update(p, boat, 30, 0, true, 150));
        assertEquals(30, r.update(p, boat, 60, 0, false, 150), "left the ice: paused");
        assertEquals(30, r.update(p, boat, 90, 0, true, 150), "first sample back on ice only re-arms");
        assertEquals(60, r.update(p, boat, 120, 0, true, 150));
    }

    @Test
    void rideResetsOnDismountVehicleChangeOrTeleport() {
        RideTracker r = new RideTracker();
        r.update(p, boat, 0, 0, true, 150);
        r.update(p, boat, 50, 0, true, 150);
        r.dismount(p);
        assertEquals(0, r.distance(p));
        r.update(p, boat, 0, 0, true, 150);
        r.update(p, boat, 50, 0, true, 150);
        assertEquals(0, r.update(p, UUID.randomUUID(), 100, 0, true, 150), "other boat");
        r.update(p, boat, 0, 0, true, 150);
        r.update(p, boat, 50, 0, true, 150);
        assertEquals(0, r.update(p, boat, 5000, 0, true, 150), "teleported");
    }
}

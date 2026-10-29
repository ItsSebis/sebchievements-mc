package dev.sebi.sebchievements.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RewardCooldownTest {
    private static final long HOUR = 3_600_000L;

    @Test
    void neverReclaimedMeansNoWait() {
        assertEquals(0, RewardCooldown.remainingMillis(10 * HOUR, 0, HOUR));
    }

    @Test
    void countsDownFromTheLastReclaim() {
        assertEquals(HOUR / 2, RewardCooldown.remainingMillis(HOUR + HOUR / 2, HOUR, HOUR));
    }

    @Test
    void isOverOnceTheCooldownHasPassed() {
        assertEquals(0, RewardCooldown.remainingMillis(3 * HOUR, HOUR, HOUR));
        assertEquals(0, RewardCooldown.remainingMillis(2 * HOUR, HOUR, HOUR));
    }

    @Test
    void zeroCooldownDisablesIt() {
        assertEquals(0, RewardCooldown.remainingMillis(HOUR, HOUR, 0));
    }

    @Test
    void aClockThatMovedBackwardsStillCapsTheWaitAtTheCooldown() {
        // last reclaim "in the future" (clock change): the wait never exceeds one cooldown
        assertEquals(HOUR, RewardCooldown.remainingMillis(HOUR, 2 * HOUR, HOUR));
    }
}

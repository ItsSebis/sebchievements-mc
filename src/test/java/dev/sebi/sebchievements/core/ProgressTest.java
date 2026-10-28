package dev.sebi.sebchievements.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProgressTest {
    @Test
    void formatsTimeLikeTheSpecExample() {
        assertEquals("12m 40s / 20m", Progress.time(760_000, 1_200_000).format());
    }

    @Test
    void formatsHoursAndZero() {
        assertEquals("0s / 5h", Progress.time(0, 5 * 3_600_000L).format());
        assertEquals("1h 1m 1s / 5h", Progress.time(3_661_000, 5 * 3_600_000L).format());
    }

    @Test
    void formatsCountsAndClampsToTarget() {
        assertEquals("7 / 10", Progress.count(7, 10).format());
        assertEquals("3 / 3", Progress.count(4, 3).format());
    }
}

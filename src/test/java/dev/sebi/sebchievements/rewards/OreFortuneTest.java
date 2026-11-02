package dev.sebi.sebchievements.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class OreFortuneTest {
    @Test
    void noFortuneOrNoFactorGivesNothing() {
        Random r = new Random(1);
        for (int i = 0; i < 100; i++) {
            assertEquals(0, OreFortune.extraDrops(0, 0.5, r));
            assertEquals(0, OreFortune.extraDrops(3, 0, r));
        }
    }

    @Test
    void halfEffectivenessAveragesHalfOfVanillasBonus() {
        Random r = new Random(42);
        int n = 200_000;
        long total = 0;
        for (int i = 0; i < n; i++) total += OreFortune.extraDrops(3, 0.5, r);
        // vanilla Fortune III: roll uniform in 0..4 with 0 -> 1, so mean extra = (0+0+1+2+3)/5 = 1.2; half is 0.6
        assertEquals(0.6, (double) total / n, 0.02);
    }

    @Test
    void fullFactorNeverExceedsVanillasMaximum() {
        Random r = new Random(7);
        for (int i = 0; i < 1000; i++) assertTrue(OreFortune.extraDrops(3, 1.0, r) <= 3);
    }
}

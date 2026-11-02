package dev.sebi.sebchievements.rewards;

import java.util.Random;

/** The pure Fortune maths for the auto-smelt perk. */
public final class OreFortune {
    private OreFortune() {}

    /**
     * Extra drops for a block vanilla gives no Fortune bonus (ancient debris): vanilla's ore roll
     * ({@code max(1, nextInt(level + 2))} times the base drop) gives {@code roll - 1} extra, scaled by
     * {@code factor} (0.5 = half effectiveness) and rounded up or down at random so the average is exact.
     */
    public static int extraDrops(int fortune, double factor, Random random) {
        if (fortune <= 0 || factor <= 0) return 0;
        int roll = Math.max(1, random.nextInt(fortune + 2));
        double extra = (roll - 1) * factor;
        int whole = (int) extra;
        return whole + (random.nextDouble() < extra - whole ? 1 : 0);
    }
}

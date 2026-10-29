package dev.sebi.sebchievements.rewards;

/** Reclaim cooldown arithmetic, kept free of Bukkit so it can be unit-tested. */
public final class RewardCooldown {
    private RewardCooldown() {}

    /**
     * @param lastReclaim wall-clock millis of the previous reclaim, 0 if there was none
     * @return millis left until the next reclaim is allowed, 0 if it is allowed now
     */
    public static long remainingMillis(long now, long lastReclaim, long cooldownMillis) {
        if (lastReclaim <= 0 || cooldownMillis <= 0) return 0;
        // capped so a clock that moved backwards can't lock a player out for longer than the cooldown itself
        return Math.min(cooldownMillis, Math.max(0, lastReclaim + cooldownMillis - now));
    }
}

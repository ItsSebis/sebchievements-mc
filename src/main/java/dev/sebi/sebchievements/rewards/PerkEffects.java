package dev.sebi.sebchievements.rewards;

/** Pure rules for keeping a perk effect alive without ever touching effects that came from somewhere else. */
public final class PerkEffects {
    /** How long one application lasts; refreshed every sweep, so it runs out about a second after a perk stops. */
    public static final int APPLY_TICKS = 40;

    private PerkEffects() {}

    /**
     * Is this active effect one we applied? Ours are always ambient, particle-less and short-lived, so a potion
     * with the same level (long, with particles) is left alone when a perk is turned off or revoked.
     */
    public static boolean isOurs(int amplifier, int durationTicks, boolean ambient, boolean particles, int perkAmplifier) {
        return ambient && !particles && amplifier == perkAmplifier && durationTicks <= APPLY_TICKS;
    }

    /** True if an existing effect already does the job (same or higher level that outlasts one application). */
    public static boolean alreadyCovered(int existingAmplifier, int existingDuration, int perkAmplifier) {
        return existingAmplifier >= perkAmplifier && existingDuration > APPLY_TICKS;
    }
}

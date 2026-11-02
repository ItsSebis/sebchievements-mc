package dev.sebi.sebchievements.rewards;

/**
 * A permanent perk unlocked by an advancement (see {@code PerkSpecs}, generated): a potion effect, extra Looting
 * levels, or auto-smelting of mined ores; exactly one of the three.
 *
 * @param effect       effect id, e.g. {@code minecraft:speed}; null for a looting perk
 * @param amplifier    0 is level I, 1 is level II (effect perks only)
 * @param lootingBonus Looting levels added to the player's own (0 for an effect perk)
 * @param autoSmelt    true for the auto-smelt perk (no effect, no looting)
 * @param icon         item shown in the reward menu
 */
public record PerkSpec(String effect, int amplifier, int lootingBonus, boolean autoSmelt, String name, String icon) {
    /** An effect or looting perk (never auto-smelt). */
    public PerkSpec(String effect, int amplifier, int lootingBonus, String name, String icon) {
        this(effect, amplifier, lootingBonus, false, name, icon);
    }

    public boolean isLooting() {
        return effect == null && !autoSmelt;
    }

    public boolean isAutoSmelt() {
        return autoSmelt;
    }

    /** A potion-effect perk, the only kind {@code PerkService.sweep} applies. */
    public boolean isEffect() {
        return effect != null;
    }

    /**
     * This perk with its strength replaced by config values. Null or out-of-range values keep the catalog's, and the
     * kind of perk never changes: an effect perk ignores {@code lootingBonus} and a looting perk ignores
     * {@code amplifier}.
     */
    public PerkSpec tuned(Integer amplifier, Integer lootingBonus) {
        int amp = isEffect() && amplifier != null && amplifier >= 0 && amplifier <= 9 ? amplifier : this.amplifier;
        int looting = isLooting() && lootingBonus != null && lootingBonus >= 1 && lootingBonus <= 10
                ? lootingBonus : this.lootingBonus;
        return new PerkSpec(effect, amp, looting, autoSmelt, name, icon);
    }
}

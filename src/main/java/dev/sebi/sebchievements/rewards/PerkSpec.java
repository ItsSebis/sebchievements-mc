package dev.sebi.sebchievements.rewards;

/**
 * A permanent perk unlocked by an advancement (see {@code PerkSpecs}, generated): either a potion effect or extra
 * Looting levels, never both.
 *
 * @param effect       effect id, e.g. {@code minecraft:speed}; null for a looting perk
 * @param amplifier    0 is level I, 1 is level II (effect perks only)
 * @param lootingBonus Looting levels added to the player's own (0 for an effect perk)
 * @param icon         item shown in the reward menu
 */
public record PerkSpec(String effect, int amplifier, int lootingBonus, String name, String icon) {
    public boolean isLooting() {
        return effect == null;
    }

    /**
     * This perk with its strength replaced by config values. Null or out-of-range values keep the catalog's, and the
     * kind of perk never changes: an effect perk ignores {@code lootingBonus} and a looting perk ignores
     * {@code amplifier}.
     */
    public PerkSpec tuned(Integer amplifier, Integer lootingBonus) {
        int amp = !isLooting() && amplifier != null && amplifier >= 0 && amplifier <= 9 ? amplifier : this.amplifier;
        int looting = isLooting() && lootingBonus != null && lootingBonus >= 1 && lootingBonus <= 10
                ? lootingBonus : this.lootingBonus;
        return new PerkSpec(effect, amp, looting, name, icon);
    }
}

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
}

package dev.sebi.sebchievements.rewards;

/**
 * A permanent effect unlocked by an advancement (see {@code PerkSpecs}, generated).
 *
 * @param effect    effect id, e.g. {@code minecraft:speed}
 * @param amplifier 0 is level I, 1 is level II
 * @param icon      item shown in the reward menu
 */
public record PerkSpec(String effect, int amplifier, String name, String icon) {}

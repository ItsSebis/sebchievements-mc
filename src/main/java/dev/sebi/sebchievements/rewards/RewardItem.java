package dev.sebi.sebchievements.rewards;

import java.util.List;
import java.util.Map;

/**
 * One reward item as written in the catalog (see {@code RewardSpecs}, generated).
 *
 * @param item         item id, e.g. {@code minecraft:emerald}
 * @param name         custom name or null
 * @param enchantments enchantment id to level
 * @param potion       potion type id or null
 * @param dye          RGB colour or null
 */
public record RewardItem(String item, int count, String name, List<String> lore, Map<String, Integer> enchantments,
                         String potion, Integer dye) {}

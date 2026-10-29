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
 * @param reclaim      what getting it back from the reward menu costs: {@code free}, {@code cost} (an item of the
 *                     same material is used up) or {@code never} (consumable, only the first grant)
 */
public record RewardItem(String item, int count, String name, List<String> lore, Map<String, Integer> enchantments,
                         String potion, Integer dye, String reclaim) {}

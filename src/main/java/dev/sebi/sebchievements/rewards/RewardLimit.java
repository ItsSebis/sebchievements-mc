package dev.sebi.sebchievements.rewards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The copy cap, as pure arithmetic. Amounts are keyed by {@link #key(String, String)}: reward id plus item id, so the
 * two totems of one reward share a key (cap 2) while different items of a reward are capped separately.
 */
public final class RewardLimit {
    private RewardLimit() {}

    public static String key(String rewardId, String itemId) {
        return rewardId + "|" + itemId;
    }

    /** What a player who earned the reward may hold: the catalog count, summed per item id. */
    public static Map<String, Integer> allowance(String rewardId, List<RewardItem> items) {
        Map<String, Integer> out = new HashMap<>();
        for (RewardItem i : items) out.merge(key(rewardId, i.item()), i.count(), Integer::sum);
        return out;
    }

    /** True if holding {@code held} plus {@code incoming} stays within {@code allowed} for every incoming key. */
    public static boolean fits(Map<String, Integer> allowed, Map<String, Integer> held, Map<String, Integer> incoming) {
        for (var e : incoming.entrySet()) {
            if (e.getValue() <= 0) continue;
            if (held.getOrDefault(e.getKey(), 0) + e.getValue() > allowed.getOrDefault(e.getKey(), 0)) return false;
        }
        return true;
    }

    /** How many copies above the allowance a player holds; 0 when within it. */
    public static int excess(int allowed, int held) {
        return Math.max(0, held - allowed);
    }
}

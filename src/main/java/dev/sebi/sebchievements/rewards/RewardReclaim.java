package dev.sebi.sebchievements.rewards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The reclaim modes of reward items ({@code free}, {@code cost}, {@code never}), as pure logic. */
public final class RewardReclaim {
    public static final String FREE = "free";
    public static final String COST = "cost";
    public static final String NEVER = "never";

    private RewardReclaim() {}

    /** The items the reward menu can give back: everything that is not consumable ({@code never}). */
    public static List<RewardItem> reclaimable(List<RewardItem> items) {
        return items.stream().filter(i -> !NEVER.equals(i.reclaim())).toList();
    }

    /** What must be handed in first: item id to total count over the {@code cost} items. */
    public static Map<String, Integer> cost(List<RewardItem> items) {
        Map<String, Integer> out = new HashMap<>();
        for (RewardItem i : items) {
            if (COST.equals(i.reclaim())) out.merge(i.item(), i.count(), Integer::sum);
        }
        return out;
    }

    /**
     * Picks which of the candidate stacks pay a cost: least enchanted first, then the most damaged, so the player's
     * best items are the last to go.
     *
     * @param enchants per candidate: how many enchantments it carries
     * @param damage   per candidate: how worn it is (higher is more damaged)
     * @return candidate indexes in the order they should be used
     */
    public static List<Integer> payOrder(int[] enchants, int[] damage) {
        return java.util.stream.IntStream.range(0, enchants.length).boxed()
                .sorted(java.util.Comparator.<Integer>comparingInt(i -> enchants[i]).thenComparing(i -> -damage[i]))
                .toList();
    }
}

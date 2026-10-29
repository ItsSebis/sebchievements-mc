package dev.sebi.sebchievements.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RewardReclaimTest {
    private static RewardItem item(String id, int count, String reclaim) {
        return new RewardItem(id, count, null, List.of(), Map.of(), null, null, reclaim);
    }

    @Test
    void consumablesAreLeftOutOfWhatCanBeReclaimed() {
        List<RewardItem> warden = List.of(item("minecraft:netherite_sword", 1, "cost"), item("minecraft:echo_shard", 4, "never"));
        assertEquals(List.of(warden.get(0)), RewardReclaim.reclaimable(warden));
        assertTrue(RewardReclaim.reclaimable(List.of(item("minecraft:totem_of_undying", 1, "never"))).isEmpty());
    }

    @Test
    void freeItemsAreReclaimableAndCostNothing() {
        List<RewardItem> bell = List.of(item("minecraft:bell", 1, "free"));
        assertEquals(bell, RewardReclaim.reclaimable(bell));
        assertTrue(RewardReclaim.cost(bell).isEmpty());
    }

    @Test
    void costSumsTheCountOfTheSameBaseItem() {
        List<RewardItem> hats = List.of(item("minecraft:leather_helmet", 2, "cost"), item("minecraft:bell", 1, "free"),
                item("minecraft:leather_helmet", 1, "cost"));
        assertEquals(Map.of("minecraft:leather_helmet", 3), RewardReclaim.cost(hats));
    }

    @Test
    void payOrderSpendsThePlainestItemsFirstThenTheMostWornOnes() {
        // candidates: 0 enchanted new, 1 plain new, 2 plain half broken, 3 enchanted broken
        List<Integer> order = RewardReclaim.payOrder(new int[] {2, 0, 0, 1}, new int[] {0, 0, 200, 300});
        assertEquals(List.of(2, 1, 3, 0), order);
    }
}

package dev.sebi.sebchievements.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RewardLimitTest {
    private static RewardItem item(String id, int count) {
        return new RewardItem(id, count, null, List.of(), Map.of(), null, null);
    }

    @Test
    void allowanceSumsTheCountsOfTheSameItem() {
        Map<String, Integer> allowed = RewardLimit.allowance("totem_chain",
                List.of(item("minecraft:totem_of_undying", 1), item("minecraft:totem_of_undying", 1)));
        assertEquals(Map.of("totem_chain|minecraft:totem_of_undying", 2), allowed);
    }

    @Test
    void differentItemsOfOneRewardAreCappedSeparately() {
        Map<String, Integer> allowed = RewardLimit.allowance("pacifist",
                List.of(item("minecraft:golden_carrot", 8), item("minecraft:flowering_azalea", 1)));
        assertEquals(8, allowed.get("pacifist|minecraft:golden_carrot"));
        assertEquals(1, allowed.get("pacifist|minecraft:flowering_azalea"));
    }

    @Test
    void secondCopyDoesNotFitWhenOneIsAllowedAndHeld() {
        Map<String, Integer> allowed = Map.of("a|minecraft:emerald", 1);
        assertFalse(RewardLimit.fits(allowed, Map.of("a|minecraft:emerald", 1), Map.of("a|minecraft:emerald", 1)));
        assertTrue(RewardLimit.fits(allowed, Map.of(), Map.of("a|minecraft:emerald", 1)));
    }

    @Test
    void partOfAStackFitsOnlyUpToTheAllowance() {
        Map<String, Integer> allowed = Map.of("a|minecraft:iron_nugget", 30);
        assertTrue(RewardLimit.fits(allowed, Map.of("a|minecraft:iron_nugget", 20), Map.of("a|minecraft:iron_nugget", 10)));
        assertFalse(RewardLimit.fits(allowed, Map.of("a|minecraft:iron_nugget", 20), Map.of("a|minecraft:iron_nugget", 11)));
    }

    @Test
    void aRewardTheyHaveNotEarnedNeverFits() {
        assertFalse(RewardLimit.fits(Map.of(), Map.of(), Map.of("a|minecraft:emerald", 1)));
    }

    @Test
    void unrelatedIncomingItemsAreIgnored() {
        assertTrue(RewardLimit.fits(Map.of(), Map.of(), Map.of("a|minecraft:emerald", 0)));
    }

    @Test
    void excessIsNeverNegative() {
        assertEquals(0, RewardLimit.excess(2, 1));
        assertEquals(0, RewardLimit.excess(2, 2));
        assertEquals(3, RewardLimit.excess(2, 5));
    }
}

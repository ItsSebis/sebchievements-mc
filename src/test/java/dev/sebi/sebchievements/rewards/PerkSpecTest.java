package dev.sebi.sebchievements.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PerkSpecTest {
    private static final PerkSpec SPEED = new PerkSpec("minecraft:speed", 1, 0, "Legs", "minecraft:sugar");
    private static final PerkSpec LOOT = new PerkSpec(null, 0, 5, "Plunderer", "minecraft:wither_skeleton_skull");

    @Test
    void configChangesTheAmplifierOfAnEffectPerk() {
        assertEquals(3, SPEED.tuned(3, null).amplifier());
        assertEquals(0, SPEED.tuned(0, null).amplifier(), "0 is level I and a valid value");
    }

    @Test
    void configChangesTheBonusOfALootingPerk() {
        assertEquals(8, LOOT.tuned(null, 8).lootingBonus());
    }

    @Test
    void missingOrOutOfRangeValuesKeepTheCatalogOnes() {
        assertEquals(1, SPEED.tuned(null, null).amplifier());
        assertEquals(1, SPEED.tuned(12, null).amplifier());
        assertEquals(1, SPEED.tuned(-1, null).amplifier());
        assertEquals(5, LOOT.tuned(null, 0).lootingBonus());
        assertEquals(5, LOOT.tuned(null, 11).lootingBonus());
    }

    @Test
    void aPerkNeverChangesKind() {
        PerkSpec effect = SPEED.tuned(2, 9);
        assertEquals(0, effect.lootingBonus());
        assertEquals("minecraft:speed", effect.effect());
        PerkSpec looting = LOOT.tuned(4, 9);
        assertEquals(0, looting.amplifier());
        assertEquals(9, looting.lootingBonus());
    }

    @Test
    void autoSmeltIsItsOwnKindAndNeverChanges() {
        PerkSpec smelt = new PerkSpec(null, 0, 0, true, "Smelter", "minecraft:blast_furnace");
        assertEquals(true, smelt.isAutoSmelt());
        assertEquals(false, smelt.isLooting());
        assertEquals(false, smelt.isEffect());
        PerkSpec tuned = smelt.tuned(3, 7);
        assertEquals(true, tuned.isAutoSmelt());
        assertEquals(0, tuned.amplifier());
        assertEquals(0, tuned.lootingBonus());
        assertEquals(false, SPEED.isAutoSmelt());
        assertEquals(true, LOOT.isLooting());
    }
}

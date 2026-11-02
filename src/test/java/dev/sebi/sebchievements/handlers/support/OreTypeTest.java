package dev.sebi.sebchievements.handlers.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class OreTypeTest {
    @Test
    void deepslateVariantsShareTheirKind() {
        assertEquals(OreType.IRON, OreType.of(Material.IRON_ORE).orElseThrow());
        assertEquals(OreType.IRON, OreType.of(Material.DEEPSLATE_IRON_ORE).orElseThrow());
        assertEquals(OreType.ANCIENT_DEBRIS, OreType.of(Material.ANCIENT_DEBRIS).orElseThrow());
    }

    @Test
    void nonOresAndRawItemsAreNotOres() {
        assertTrue(OreType.of(Material.STONE).isEmpty());
        assertTrue(OreType.of(Material.RAW_IRON_BLOCK).isEmpty());
        assertTrue(OreType.of(Material.IRON_BLOCK).isEmpty());
    }

    @Test
    void keysAreValidForTheDataStore() {
        for (OreType t : OreType.values()) assertTrue(t.key().matches("[a-z0-9_]+"), t.key());
        assertEquals(11, OreType.values().length);
    }
}

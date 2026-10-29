package dev.sebi.sebchievements.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlacedBlocksTest {
    private static final long MIN = 60_000;
    private static final UUID WORLD = UUID.randomUUID();

    @Test
    void aBlockCountsAsPlacedUntilTheWindowPasses() {
        PlacedBlocks p = new PlacedBlocks(30 * MIN);
        p.placed(WORLD, 1, 64, 1, 0);
        assertTrue(p.recentlyPlaced(WORLD, 1, 64, 1, 29 * MIN));
        assertFalse(p.recentlyPlaced(WORLD, 1, 64, 1, 30 * MIN));
    }

    @Test
    void positionsAndWorldsAreSeparate() {
        PlacedBlocks p = new PlacedBlocks(30 * MIN);
        p.placed(WORLD, 1, 64, 1, 0);
        assertFalse(p.recentlyPlaced(WORLD, 2, 64, 1, MIN));
        assertFalse(p.recentlyPlaced(UUID.randomUUID(), 1, 64, 1, MIN));
    }

    @Test
    void placingAgainRestartsTheWindow() {
        PlacedBlocks p = new PlacedBlocks(30 * MIN);
        p.placed(WORLD, 1, 64, 1, 0);
        p.placed(WORLD, 1, 64, 1, 25 * MIN);
        assertTrue(p.recentlyPlaced(WORLD, 1, 64, 1, 50 * MIN));
    }

    @Test
    void purgeForgetsOnlyExpiredPositions() {
        PlacedBlocks p = new PlacedBlocks(30 * MIN);
        p.placed(WORLD, 1, 64, 1, 0);
        p.placed(WORLD, 2, 64, 1, 20 * MIN);
        p.purge(35 * MIN);
        assertEquals(1, p.size());
        assertTrue(p.recentlyPlaced(WORLD, 2, 64, 1, 35 * MIN));
    }
}

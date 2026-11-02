package dev.sebi.sebchievements.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlacedDebrisTest {
    @Test
    void positionsInOneChunkAreDistinctAndWrapPerChunk() {
        assertNotEquals(PlacedDebris.pack(1, 10, 2), PlacedDebris.pack(2, 10, 1));
        assertNotEquals(PlacedDebris.pack(1, 10, 2), PlacedDebris.pack(1, 11, 2));
        assertNotEquals(PlacedDebris.pack(1, -60, 2), PlacedDebris.pack(1, -61, 2));
        assertEquals(PlacedDebris.pack(1, 10, 2), PlacedDebris.pack(17, 10, -14), "same spot in another chunk's local coordinates");
    }

    @Test
    void addAndRemoveKeepTheSetExact() {
        long a = PlacedDebris.pack(1, 10, 2), b = PlacedDebris.pack(3, 10, 4);
        long[] set = PlacedDebris.with(PlacedDebris.with(null, a), b);
        assertArrayEquals(new long[] {a, b}, set);
        assertArrayEquals(set, PlacedDebris.with(set, a), "adding twice changes nothing");
        assertTrue(PlacedDebris.contains(set, b));
        assertArrayEquals(new long[] {b}, PlacedDebris.without(set, a));
        assertFalse(PlacedDebris.contains(PlacedDebris.without(set, a), a));
        assertEquals(0, PlacedDebris.without(new long[] {a}, a).length);
        assertFalse(PlacedDebris.contains(null, a));
    }
}

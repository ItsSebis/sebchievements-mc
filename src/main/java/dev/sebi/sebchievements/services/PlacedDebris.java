package dev.sebi.sebchievements.services;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

/**
 * Ancient debris a player placed, remembered until it is broken again (or blown up, or moved by a piston). Unlike
 * {@link PlacedBlocks} this is persistent, but the storage stays tiny because only standing placed debris is kept: it
 * lives in the PersistentDataContainer of the chunk the block is in, so it needs no database and disappears with
 * the block. Debris is the one ore that drops itself without Silk Touch, so it is the only one that needs this.
 */
public final class PlacedDebris {
    private final NamespacedKey key;

    public PlacedDebris(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "placed_debris");
    }

    public void add(Block b) {
        PersistentDataContainer pdc = b.getChunk().getPersistentDataContainer();
        long[] cur = pdc.get(key, PersistentDataType.LONG_ARRAY);
        pdc.set(key, PersistentDataType.LONG_ARRAY, with(cur, pack(b.getX(), b.getY(), b.getZ())));
    }

    /** Forgets the block; true if it was a placed one. */
    public boolean take(Block b) {
        PersistentDataContainer pdc = b.getChunk().getPersistentDataContainer();
        long[] cur = pdc.get(key, PersistentDataType.LONG_ARRAY);
        long pos = pack(b.getX(), b.getY(), b.getZ());
        if (!contains(cur, pos)) return false;
        long[] left = without(cur, pos);
        if (left.length == 0) pdc.remove(key);
        else pdc.set(key, PersistentDataType.LONG_ARRAY, left);
        return true;
    }

    /** A position inside its chunk: height in the high bits, then x and z (0-15) in one byte. */
    static long pack(int x, int y, int z) {
        return ((long) y << 8) | ((x & 15) << 4) | (z & 15);
    }

    static boolean contains(long[] positions, long pos) {
        if (positions == null) return false;
        for (long p : positions) if (p == pos) return true;
        return false;
    }

    static long[] with(long[] positions, long pos) {
        if (positions == null) return new long[] {pos};
        if (contains(positions, pos)) return positions;
        long[] out = Arrays.copyOf(positions, positions.length + 1);
        out[positions.length] = pos;
        return out;
    }

    static long[] without(long[] positions, long pos) {
        if (positions == null) return new long[0];
        return Arrays.stream(positions).filter(p -> p != pos).toArray();
    }
}

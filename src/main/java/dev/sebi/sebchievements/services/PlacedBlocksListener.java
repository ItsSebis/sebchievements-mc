package dev.sebi.sebchievements.services;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/** Feeds {@link PlacedBlocks}. */
public final class PlacedBlocksListener implements Listener {
    private final PlacedBlocks placed;

    public PlacedBlocksListener(PlacedBlocks placed) {
        this.placed = placed;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        placed.placed(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ(), System.currentTimeMillis());
    }
}

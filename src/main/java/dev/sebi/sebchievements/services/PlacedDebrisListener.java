package dev.sebi.sebchievements.services;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Feeds {@link PlacedDebris}: placing adds, explosions forget, and a piston that pushes or pulls a tracked block
 * moves its entry along, so moving placed debris around doesn't launder it. Breaking by a player is handled where it
 * is counted.
 */
public final class PlacedDebrisListener implements Listener {
    private final PlacedDebris placed;

    public PlacedDebrisListener(PlacedDebris placed) {
        this.placed = placed;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() == Material.ANCIENT_DEBRIS) placed.add(e.getBlockPlaced());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        forget(e.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent e) {
        forget(e.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExtend(BlockPistonExtendEvent e) {
        move(e.getBlocks(), e.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRetract(BlockPistonRetractEvent e) {
        // the event's direction is the way the piston faces; pulled blocks move the other way
        move(e.getBlocks(), e.getDirection().getOppositeFace());
    }

    private void forget(List<Block> blocks) {
        for (Block b : blocks) if (b.getType() == Material.ANCIENT_DEBRIS) placed.take(b);
    }

    private void move(List<Block> blocks, BlockFace face) {
        List<Block> moved = new ArrayList<>();
        for (Block b : blocks) {
            if (b.getType() == Material.ANCIENT_DEBRIS && placed.take(b)) moved.add(b.getRelative(face));
        }
        moved.forEach(placed::add);
    }
}

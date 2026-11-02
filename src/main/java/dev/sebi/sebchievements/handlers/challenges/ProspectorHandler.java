package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.core.Progress;
import dev.sebi.sebchievements.core.ProgressReporting;
import dev.sebi.sebchievements.handlers.support.OreType;
import dev.sebi.sebchievements.services.PlacedDebris;
import dev.sebi.sebchievements.services.PlacedDebrisListener;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mine every kind of ore {@code per_ore} times. The counts start from the game's own "mined" statistics, read once
 * when the plugin first sees the player. Silk Touch breaks don't count, which covers every ore that can be placed
 * again; ancient debris drops itself regardless, so placed debris is remembered until broken ({@link PlacedDebris})
 * and doesn't count.
 */
public final class ProspectorHandler implements AdvancementHandler, ProgressReporting, Listener {
    private static final String SEEDED_KEY = "statseed/" + AdvancementIds.PROSPECTOR;

    private HandlerContext ctx;
    private long target;
    private PlacedDebris placed;

    @Override
    public String id() {
        return AdvancementIds.PROSPECTOR;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.target = ctx.config().getLong("per_ore", 64);
        this.placed = new PlacedDebris(ctx.plugin());
        ctx.services().tick().register(ctx.id(), this::seed);
        ctx.listen(new PlacedDebrisListener(placed));
        ctx.listen(this);
    }

    static String key(OreType type) {
        return "ore_" + type.key() + "/" + AdvancementIds.PROSPECTOR;
    }

    /** Adds the player's earlier "mined" statistics once. */
    private void seed(Player p) {
        if (ctx.store().getLong(p, SEEDED_KEY, 0) != 0) return;
        ctx.store().setLong(p, SEEDED_KEY, 1);
        for (OreType t : OreType.values()) {
            long before = earlier(p, t);
            if (before > 0) ctx.store().addLong(p, key(t), before);
        }
        checkDone(p);
    }

    private static long earlier(Player p, OreType t) {
        long sum = 0;
        for (Material m : t.materials()) sum += p.getStatistic(Statistic.MINE_BLOCK, m);
        return sum;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Optional<OreType> found = OreType.of(b.getType());
        if (found.isEmpty()) return;
        OreType type = found.get();
        // forget a placed debris on every break, whoever breaks it, so the entry never outlives the block
        boolean wasPlaced = type == OreType.ANCIENT_DEBRIS && placed.take(b);
        Player p = e.getPlayer();
        if (wasPlaced || (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE)) return;
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (type != OreType.ANCIENT_DEBRIS && tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0) return;
        if (!b.isPreferredTool(tool)) return;
        // a player the plugin hasn't seeded yet would have the statistic added on top of this break, so seed first
        seed(p);
        ctx.store().addLong(p, key(type), 1);
        checkDone(p);
    }

    private void checkDone(Player p) {
        if (finished(p) == OreType.values().length) ctx.grant(p);
    }

    private long count(Player p, OreType t) {
        return ctx.store().getLong(p, key(t), 0);
    }

    private int finished(Player p) {
        int done = 0;
        for (OreType t : OreType.values()) if (count(p, t) >= target) done++;
        return done;
    }

    @Override
    public Optional<Progress> progress(Player player) {
        return Optional.of(Progress.count(finished(player), OreType.values().length));
    }

    @Override
    public List<String> debug(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("ores finished = " + finished(player) + " / " + OreType.values().length + " (" + target + " each)");
        for (OreType t : OreType.values()) {
            lines.add(t.label() + " = " + count(player, t) + " / " + target + " (mined stat " + earlier(player, t) + ")");
        }
        lines.add("seeded = " + (ctx.store().getLong(player, SEEDED_KEY, 0) != 0));
        return lines;
    }
}

package dev.sebi.sebchievements.rewards;

import dev.sebi.sebchievements.handlers.support.OreType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * The auto-smelt perk: what a mined ore drops is replaced by its furnace result (raw iron becomes an ingot). Vanilla
 * has already applied the player's Fortune to the drop, so the stack size carries over. Ancient debris gets no Fortune
 * in vanilla, so it receives {@code perks.<id>.debris_fortune_factor} of a Fortune roll on top. The furnace
 * experience is spawned as well, so the perk costs nothing. Silk Touch breaks are left alone.
 */
public final class AutoSmeltPerkListener implements Listener {
    private record Smelt(Material result, float experience) {}

    private static final Map<Material, Optional<Smelt>> CACHE = new EnumMap<>(Material.class);

    private final PerkService perks;
    private final Plugin plugin;
    private final Random random = new Random();

    public AutoSmeltPerkListener(PerkService perks, Plugin plugin) {
        this.perks = perks;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) return;
        Optional<OreType> ore = OreType.of(e.getBlockState().getType());
        if (ore.isEmpty() || !perks.autoSmelt(p)) return;
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0) return;

        int fortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
        double factor = plugin.getConfig().getDouble("perks.prospector.debris_fortune_factor", 0.5);
        double experience = 0;
        for (Item item : e.getItems()) {
            ItemStack stack = item.getItemStack();
            int amount = stack.getAmount();
            if (ore.get() == OreType.ANCIENT_DEBRIS) amount += OreFortune.extraDrops(fortune, factor, random);
            Optional<Smelt> smelt = smelt(stack.getType());
            if (smelt.isEmpty()) {
                stack.setAmount(amount);
            } else {
                stack = new ItemStack(smelt.get().result(), amount);
                experience += smelt.get().experience() * amount;
            }
            item.setItemStack(stack);
        }
        int orbs = (int) experience + (random.nextDouble() < experience - (int) experience ? 1 : 0);
        if (orbs > 0) {
            e.getBlock().getWorld().spawn(e.getBlock().getLocation().add(0.5, 0.5, 0.5), ExperienceOrb.class,
                    orb -> orb.setExperience(orbs));
        }
    }

    /** What a furnace or blast furnace makes of the item, if anything. */
    private static Optional<Smelt> smelt(Material m) {
        return CACHE.computeIfAbsent(m, material -> {
            for (var it = Bukkit.recipeIterator(); it.hasNext(); ) {
                if (it.next() instanceof CookingRecipe<?> r && (r instanceof FurnaceRecipe || r instanceof BlastingRecipe)
                        && r.getInputChoice().test(new ItemStack(material))) {
                    return Optional.of(new Smelt(r.getResult().getType(), r.getExperience()));
                }
            }
            return Optional.empty();
        });
    }
}

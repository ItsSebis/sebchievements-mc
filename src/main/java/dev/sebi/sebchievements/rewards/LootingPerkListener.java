package dev.sebi.sebchievements.rewards;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Looting perks. Vanilla loot tables read the killer's real enchantment levels and the API's looting modifier is
 * ignored, so the mob's table is rolled again while the killer's main hand briefly holds a copy of the item with
 * Looting raised by the perk. The item is put back before anything else can run, so durability and enchantments are
 * untouched. The new roll replaces what the table dropped; equipment drops stay.
 */
public final class LootingPerkListener implements Listener {
    private static final int SAMPLES = 2_000;
    /** High enough that even rare entries show up in the samples that find out what a table can drop. */
    private static final int SAMPLE_LOOTING = 10;

    private static final Map<Material, Material> SMELTED = new EnumMap<>(Material.class);

    private final PerkService perks;
    private final Map<EntityType, Set<Material>> producible = new EnumMap<>(EntityType.class);

    public LootingPerkListener(PerkService perks) {
        this.perks = perks;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Lootable lootable) || e.getEntity() instanceof Player) return;
        LootTable table = lootable.getLootTable();
        Player killer = e.getEntity().getKiller();
        if (table == null || killer == null) return;
        if (killer.getGameMode() != GameMode.SURVIVAL && killer.getGameMode() != GameMode.ADVENTURE) return;
        int bonus = perks.lootingBonus(killer);
        if (bonus <= 0) return;

        LivingEntity mob = e.getEntity();
        int level = killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING) + bonus;
        Set<Material> replaced = producible.computeIfAbsent(mob.getType(), t -> sample(table, mob, killer));
        boolean burning = mob.getFireTicks() > 0
                || killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.FIRE_ASPECT) > 0;
        Collection<ItemStack> boosted = roll(table, mob, killer, level, 1);
        if (burning) boosted.forEach(LootingPerkListener::cook);
        // a burning kill's vanilla drops are already cooked, so their cooked forms count as the table's own
        e.getDrops().removeIf(s -> replaced.contains(s.getType()) || replaced.contains(raw(s.getType())));
        e.getDrops().addAll(boosted);
    }

    /** Smelts a food drop in place, like the table's furnace_smelt does for a burning mob. */
    private static void cook(ItemStack stack) {
        Material result = SMELTED.computeIfAbsent(stack.getType(), m -> {
            for (var it = Bukkit.recipeIterator(); it.hasNext(); ) {
                if (it.next() instanceof FurnaceRecipe f && f.getInputChoice().test(new ItemStack(m))
                        && f.getResult().getType().isEdible()) {
                    return f.getResult().getType();
                }
            }
            return m;
        });
        stack.setType(result);
    }

    /** The raw material a cooked food drop came from, or the material itself. */
    private static Material raw(Material cooked) {
        for (var entry : SMELTED.entrySet()) if (entry.getValue() == cooked) return entry.getKey();
        return cooked;
    }

    /** Every item the table dropped over many rolls at a very high Looting level. */
    private static Set<Material> sample(LootTable table, LivingEntity mob, Player killer) {
        Set<Material> seen = EnumSet.noneOf(Material.class);
        for (ItemStack s : roll(table, mob, killer, SAMPLE_LOOTING, SAMPLES)) seen.add(s.getType());
        return seen;
    }

    /** Rolls the table {@code times} times with the killer's main hand holding Looting {@code level}, then restores it. */
    private static List<ItemStack> roll(LootTable table, LivingEntity mob, Player killer, int level, int times) {
        ItemStack original = killer.getInventory().getItemInMainHand();
        ItemStack temp = original.isEmpty() ? new ItemStack(Material.STICK) : original.clone();
        temp.addUnsafeEnchantment(Enchantment.LOOTING, level);
        Random random = new Random();
        List<ItemStack> out = new ArrayList<>();
        killer.getInventory().setItemInMainHand(temp);
        try {
            for (int i = 0; i < times; i++) {
                LootContext ctx = new LootContext.Builder(mob.getLocation()).lootedEntity(mob).killer(killer).build();
                out.addAll(table.populateLoot(random, ctx));
            }
        } finally {
            killer.getInventory().setItemInMainHand(original);
        }
        return out;
    }
}

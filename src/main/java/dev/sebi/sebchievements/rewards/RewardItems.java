package dev.sebi.sebchievements.rewards;

import dev.sebi.sebchievements.RewardSpecs;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

/** Builds reward ItemStacks from the catalog and recognises them again by their tag. */
public final class RewardItems {
    /** Marks a stack as the reward of an advancement; the generator writes the same key into the loot tables. */
    public static final NamespacedKey TAG = new NamespacedKey("smp", "reward_id");

    private RewardItems() {}

    /** The reward id a stack is tagged with, or null for any other item. */
    public static String idOf(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(TAG, PersistentDataType.STRING);
    }

    /** All stacks of one catalog item (a count above the stack size is split, like the loot table does). */
    public static List<ItemStack> build(String rewardId, String advancementTitle, RewardItem spec) {
        Material material = Material.matchMaterial(spec.item());
        if (material == null) throw new IllegalStateException("Unknown reward item " + spec.item());
        List<ItemStack> out = new ArrayList<>();
        int left = spec.count();
        int max = Math.max(1, material.getMaxStackSize());
        while (left > 0) {
            int n = Math.min(left, max);
            out.add(single(rewardId, advancementTitle, spec, material, n));
            left -= n;
        }
        return out;
    }

    /** Every stack of an advancement's reward, in catalog order. */
    public static List<ItemStack> buildAll(String rewardId, String advancementTitle) {
        List<ItemStack> out = new ArrayList<>();
        for (RewardItem spec : RewardSpecs.ITEMS.getOrDefault(rewardId, List.of())) {
            out.addAll(build(rewardId, advancementTitle, spec));
        }
        return out;
    }

    private static ItemStack single(String rewardId, String title, RewardItem spec, Material material, int amount) {
        ItemStack stack = new ItemStack(material, amount);
        ItemMeta meta = stack.getItemMeta();
        if (spec.name() != null) meta.customName(plain(spec.name(), null));
        List<Component> lore = new ArrayList<>();
        for (String line : spec.lore()) lore.add(plain(line, NamedTextColor.GRAY));
        lore.add(plain("Sebchievement: " + title, NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        if (spec.dye() != null && meta instanceof LeatherArmorMeta leather) leather.setColor(Color.fromRGB(spec.dye()));
        for (var e : spec.enchantments().entrySet()) {
            Enchantment enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                    .get(NamespacedKey.fromString(e.getKey()));
            if (enchantment == null) throw new IllegalStateException("Unknown enchantment " + e.getKey());
            if (meta instanceof EnchantmentStorageMeta book) book.addStoredEnchant(enchantment, e.getValue(), true);
            else meta.addEnchant(enchantment, e.getValue(), true);
        }
        if (spec.potion() != null && meta instanceof PotionMeta potion) {
            PotionType type = Registry.POTION.get(NamespacedKey.fromString(spec.potion()));
            if (type == null) throw new IllegalStateException("Unknown potion " + spec.potion());
            potion.setBasePotionType(type);
        }
        meta.getPersistentDataContainer().set(TAG, PersistentDataType.STRING, rewardId);
        stack.setItemMeta(meta);
        return stack;
    }

    /** A text component that is not italic (custom names and lore are italic by default). */
    public static Component plain(String text, NamedTextColor color) {
        Component c = Component.text(text).decoration(TextDecoration.ITALIC, false);
        return color == null ? c : c.color(color);
    }
}

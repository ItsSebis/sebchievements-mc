package dev.sebi.sebchievements.handlers.support;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The ore kinds the Prospector advancement and the auto-smelt perk know; a deepslate variant is the same kind. */
public enum OreType {
    COAL("Coal", Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE),
    COPPER("Copper", Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE),
    IRON("Iron", Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE),
    GOLD("Gold", Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE),
    REDSTONE("Redstone", Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE),
    LAPIS("Lapis", Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE),
    DIAMOND("Diamond", Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE),
    EMERALD("Emerald", Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE),
    QUARTZ("Nether quartz", Material.NETHER_QUARTZ_ORE),
    NETHER_GOLD("Nether gold", Material.NETHER_GOLD_ORE),
    ANCIENT_DEBRIS("Ancient debris", Material.ANCIENT_DEBRIS);

    private static final Map<Material, OreType> BY_MATERIAL = new EnumMap<>(Material.class);

    static {
        for (OreType t : values()) for (Material m : t.materials) BY_MATERIAL.put(m, t);
    }

    private final String label;
    private final List<Material> materials;

    OreType(String label, Material... materials) {
        this.label = label;
        this.materials = Arrays.asList(materials);
    }

    /** E.g. {@code Nether gold}. */
    public String label() {
        return label;
    }

    /** Lowercase key part, e.g. {@code nether_gold}. */
    public String key() {
        return name().toLowerCase();
    }

    public List<Material> materials() {
        return materials;
    }

    public static Optional<OreType> of(Material m) {
        return Optional.ofNullable(BY_MATERIAL.get(m));
    }
}

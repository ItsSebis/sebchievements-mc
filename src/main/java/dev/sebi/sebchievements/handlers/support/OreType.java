package dev.sebi.sebchievements.handlers.support;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The ore kinds the Prospector advancement and the auto-smelt perk know; a deepslate variant is the same kind. */
public enum OreType {
    COAL("Coal", 64, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE),
    COPPER("Copper", 64, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE),
    IRON("Iron", 64, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE),
    GOLD("Gold", 48, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE),
    REDSTONE("Redstone", 64, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE),
    LAPIS("Lapis", 64, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE),
    DIAMOND("Diamond", 32, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE),
    EMERALD("Emerald", 16, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE),
    QUARTZ("Nether quartz", 64, Material.NETHER_QUARTZ_ORE),
    NETHER_GOLD("Nether gold", 64, Material.NETHER_GOLD_ORE),
    ANCIENT_DEBRIS("Ancient debris", 24, Material.ANCIENT_DEBRIS);

    private static final Map<Material, OreType> BY_MATERIAL = new EnumMap<>(Material.class);

    static {
        for (OreType t : values()) for (Material m : t.materials) BY_MATERIAL.put(m, t);
    }

    private final String label;
    private final long defaultTarget;
    private final List<Material> materials;

    OreType(String label, long defaultTarget, Material... materials) {
        this.label = label;
        this.defaultTarget = defaultTarget;
        this.materials = Arrays.asList(materials);
    }

    /** E.g. {@code Nether gold}. */
    public String label() {
        return label;
    }

    /** How many of this ore Prospector asks for unless {@code targets.<key>} in config.yml says otherwise. */
    public long defaultTarget() {
        return defaultTarget;
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

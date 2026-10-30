package dev.sebi.sebchievements.rewards;

import dev.sebi.sebchievements.PerkSpecs;
import dev.sebi.sebchievements.core.PlayerDataStore;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Effect perks: while the advancement is earned and the player has not switched the perk off, the effect is re-applied
 * every sweep for {@link PerkEffects#APPLY_TICKS} ticks. Nothing is stored about the effect itself, so milk, death or a
 * relog just get it reapplied within a second, and revoking the advancement or switching the perk off lets it run out.
 */
public final class PerkService {
    private final Plugin plugin;
    private final RewardService rewards;
    private final PlayerDataStore store;

    public PerkService(Plugin plugin, RewardService rewards, PlayerDataStore store) {
        this.plugin = plugin;
        this.rewards = rewards;
        this.store = store;
    }

    /** The perk's catalog spec with the strength from {@code perks.<id>} in config.yml (amplifier or looting_bonus). */
    public PerkSpec spec(String id) {
        PerkSpec catalog = PerkSpecs.PERKS.get(id);
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("perks." + id);
        if (catalog == null || section == null) return catalog;
        return catalog.tuned(section.isInt("amplifier") ? section.getInt("amplifier") : null,
                section.isInt("looting_bonus") ? section.getInt("looting_bonus") : null);
    }

    public boolean earned(Player p, String id) {
        return rewards.earned(p, id);
    }

    /** Perks are on by default; the key only exists while a player has switched one off. */
    public boolean enabled(Player p, String id) {
        return store.getLong(p, offKey(id), 0) == 0;
    }

    public boolean toggle(Player p, String id) {
        boolean nowOn = !enabled(p, id);
        if (nowOn) store.remove(p, offKey(id));
        else store.setLong(p, offKey(id), 1);
        return nowOn;
    }

    /** The largest Looting bonus among the looting perks the player has earned and left switched on, 0 for none. */
    public int lootingBonus(Player p) {
        int best = 0;
        for (String id : PerkSpecs.PERKS.keySet()) {
            PerkSpec spec = spec(id);
            if (spec.isLooting() && earned(p, id) && enabled(p, id)) best = Math.max(best, spec.lootingBonus());
        }
        return best;
    }

    /** E.g. {@code 1 of 2 perks on (2 unlocked)}, for the menu button. */
    public String summary(Player p) {
        int unlocked = 0, on = 0;
        for (String id : PerkSpecs.PERKS.keySet()) {
            if (!earned(p, id)) continue;
            unlocked++;
            if (enabled(p, id)) on++;
        }
        return on + " of " + PerkSpecs.PERKS.size() + " perks on (" + unlocked + " unlocked)";
    }

    /** Runs from the shared TickScheduler for every online player. */
    public void sweep(Player p) {
        if (p.isDead()) return;
        PerkSpecs.PERKS.keySet().forEach(id -> {
            PerkSpec spec = spec(id);
            if (spec.isLooting()) return;
            PotionEffectType type = type(spec);
            if (type == null) return;
            if (earned(p, id) && enabled(p, id)) apply(p, spec, type);
            else remove(p, spec, type);
        });
    }

    private void apply(Player p, PerkSpec spec, PotionEffectType type) {
        PotionEffect existing = p.getPotionEffect(type);
        if (existing != null && PerkEffects.alreadyCovered(existing.getAmplifier(), existing.getDuration(), spec.amplifier())) return;
        p.addPotionEffect(new PotionEffect(type, PerkEffects.APPLY_TICKS, spec.amplifier(), true, false, true));
    }

    private void remove(Player p, PerkSpec spec, PotionEffectType type) {
        PotionEffect existing = p.getPotionEffect(type);
        if (existing != null && PerkEffects.isOurs(existing.getAmplifier(), existing.getDuration(), existing.isAmbient(),
                existing.hasParticles(), spec.amplifier())) {
            p.removePotionEffect(type);
        }
    }

    static PotionEffectType type(PerkSpec spec) {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).get(NamespacedKey.fromString(spec.effect()));
    }

    static String offKey(String id) {
        return "perk_off/" + id;
    }
}

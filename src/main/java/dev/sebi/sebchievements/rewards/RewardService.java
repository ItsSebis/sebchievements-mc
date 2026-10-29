package dev.sebi.sebchievements.rewards;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.RewardSpecs;
import dev.sebi.sebchievements.core.PlayerDataStore;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The rules of the reward menu: who may hold how many copies, the reclaim cooldown, and handing items out. */
public final class RewardService {
    /** Skips the copy cap. */
    public static final String PERM_MULTIPLE = "smpadv.rewards.multiple";
    /** Skips the reclaim cooldown. */
    public static final String PERM_NO_COOLDOWN = "smpadv.rewards.nocooldown";

    public enum Status { OK, NOT_EARNED, COOLDOWN, HOLDING, NO_SPACE }

    /** @param waitMillis the remaining cooldown, only set for {@link Status#COOLDOWN} */
    public record Result(Status status, long waitMillis) {
        static Result of(Status status) {
            return new Result(status, 0);
        }
    }

    private final Plugin plugin;
    private final PlayerDataStore store;

    public RewardService(Plugin plugin, PlayerDataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public boolean unlimited(Player p) {
        return p.hasPermission(PERM_MULTIPLE);
    }

    public boolean earned(Player p, String id) {
        Advancement adv = Bukkit.getAdvancement(AdvancementIds.key(id));
        return adv != null && p.getAdvancementProgress(adv).isDone();
    }

    public String title(String id) {
        Advancement adv = Bukkit.getAdvancement(AdvancementIds.key(id));
        if (adv == null || adv.getDisplay() == null) return id;
        return PlainTextComponentSerializer.plainText().serialize(adv.getDisplay().title());
    }

    /** What the player may hold of the rewards in {@code ids}: the catalog amounts for those they have earned. */
    public Map<String, Integer> allowed(Player p, Set<String> ids) {
        Map<String, Integer> out = new HashMap<>();
        for (String id : ids) {
            List<RewardItem> items = RewardSpecs.ITEMS.get(id);
            if (items == null || !earned(p, id)) continue;
            RewardLimit.allowance(id, items).forEach((k, v) -> out.merge(k, v, Integer::sum));
        }
        return out;
    }

    /** Would holding {@code incoming} on top of what the player has stay within their allowance? */
    public boolean canReceive(Player p, ItemStack incoming) {
        Map<String, Integer> coming = RewardScan.inStack(incoming);
        if (coming.isEmpty()) return true;
        return RewardLimit.fits(allowed(p, idsOf(coming.keySet())), RewardScan.held(p), coming);
    }

    /** Removes copies above the allowance; returns how many were removed. */
    public int enforce(Player p) {
        Map<String, Integer> held = RewardScan.held(p);
        if (held.isEmpty()) return 0;
        Map<String, Integer> allowed = allowed(p, idsOf(held.keySet()));
        Map<String, Integer> excess = new HashMap<>();
        held.forEach((key, amount) -> {
            int over = RewardLimit.excess(allowed.getOrDefault(key, 0), amount);
            if (over > 0) excess.put(key, over);
        });
        return excess.isEmpty() ? 0 : RewardScan.remove(p, excess);
    }

    public long cooldownRemaining(Player p, String id) {
        if (p.hasPermission(PERM_NO_COOLDOWN)) return 0;
        long last = store.getLong(p, reclaimKey(id), 0);
        return RewardCooldown.remainingMillis(System.currentTimeMillis(), last, cooldownMillis());
    }

    /** Hands the reward of an earned advancement back to a player, if the cap and cooldown allow it. */
    public Result reclaim(Player p, String id) {
        if (!RewardSpecs.ITEMS.containsKey(id) || !earned(p, id)) return Result.of(Status.NOT_EARNED);
        long wait = cooldownRemaining(p, id);
        if (wait > 0) return new Result(Status.COOLDOWN, wait);

        List<ItemStack> stacks = RewardItems.buildAll(id, title(id));
        if (!unlimited(p)) {
            Map<String, Integer> coming = new HashMap<>();
            stacks.forEach(s -> coming.merge(RewardLimit.key(id, s.getType().getKey().toString()), s.getAmount(), Integer::sum));
            Map<String, Integer> allowed = RewardLimit.allowance(id, RewardSpecs.ITEMS.get(id));
            if (!RewardLimit.fits(allowed, RewardScan.held(p), coming)) return Result.of(Status.HOLDING);
        }
        if (!hasSpaceFor(p, stacks)) return Result.of(Status.NO_SPACE);

        p.getInventory().addItem(stacks.toArray(ItemStack[]::new));
        if (!p.hasPermission(PERM_NO_COOLDOWN)) store.setLong(p, reclaimKey(id), System.currentTimeMillis());
        return Result.of(Status.OK);
    }

    /** Admin menu: gives the reward with no checks and no cooldown; what does not fit lands at the player's feet. */
    public void give(Player p, String id) {
        for (ItemStack leftover : p.getInventory().addItem(RewardItems.buildAll(id, title(id)).toArray(ItemStack[]::new)).values()) {
            p.getWorld().dropItem(p.getLocation(), leftover);
        }
    }

    /** True if the player already holds a full set of this reward, for the menu lore. */
    public boolean holdsAll(Player p, String id, Map<String, Integer> held) {
        List<RewardItem> items = RewardSpecs.ITEMS.getOrDefault(id, List.of());
        Map<String, Integer> allowed = RewardLimit.allowance(id, items);
        return !allowed.isEmpty() && allowed.entrySet().stream()
                .allMatch(e -> held.getOrDefault(e.getKey(), 0) >= e.getValue());
    }

    private long cooldownMillis() {
        return plugin.getConfig().getLong("rewards.reclaim_cooldown_minutes", 60) * 60_000L;
    }

    static String reclaimKey(String id) {
        return "reward_reclaim/" + id;
    }

    private static Set<String> idsOf(Set<String> keys) {
        Set<String> ids = new HashSet<>();
        for (String k : keys) ids.add(k.substring(0, k.indexOf('|')));
        return ids;
    }

    /** Dry run on a copy of the main inventory, so nothing is handed out unless all of it fits. */
    private static boolean hasSpaceFor(Player p, List<ItemStack> stacks) {
        Inventory scratch = Bukkit.createInventory(null, 36);
        ItemStack[] storage = p.getInventory().getStorageContents();
        for (int i = 0; i < storage.length && i < 36; i++) scratch.setItem(i, storage[i] == null ? null : storage[i].clone());
        return scratch.addItem(stacks.stream().map(ItemStack::clone).toArray(ItemStack[]::new)).isEmpty();
    }
}

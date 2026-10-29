package dev.sebi.sebchievements.rewards;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.RewardSpecs;
import dev.sebi.sebchievements.core.PlayerDataStore;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Date;
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

    public enum Status { OK, NOT_EARNED, NOT_RECLAIMABLE, COOLDOWN, HOLDING, MISSING_COST, NO_SPACE }

    /**
     * @param waitMillis the remaining cooldown, only set for {@link Status#COOLDOWN}
     * @param detail     what is missing, e.g. {@code 1x Shield}, only set for {@link Status#MISSING_COST}
     */
    public record Result(Status status, long waitMillis, String detail) {
        static Result of(Status status) {
            return new Result(status, 0, null);
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

    /** When the player completed the advancement (latest criterion), 0 if unknown. */
    public long earnedAt(Player p, String id) {
        Advancement adv = Bukkit.getAdvancement(AdvancementIds.key(id));
        if (adv == null) return 0;
        AdvancementProgress progress = p.getAdvancementProgress(adv);
        long latest = 0;
        for (String criterion : progress.getAwardedCriteria()) {
            Date at = progress.getDateAwarded(criterion);
            if (at != null) latest = Math.max(latest, at.getTime());
        }
        return latest;
    }

    /** The cooldown runs from the later of earning the advancement and the last reclaim. */
    public long cooldownRemaining(Player p, String id) {
        if (p.hasPermission(PERM_NO_COOLDOWN)) return 0;
        long since = Math.max(store.getLong(p, reclaimKey(id), 0), earnedAt(p, id));
        return RewardCooldown.remainingMillis(System.currentTimeMillis(), since, cooldownMillis());
    }

    /** The items of a reward that the menu can give back (consumables are left out). */
    public static List<RewardItem> reclaimable(String id) {
        return RewardReclaim.reclaimable(RewardSpecs.ITEMS.getOrDefault(id, List.of()));
    }

    /**
     * Hands the reclaimable part of an earned reward back, if the cooldown and the copy cap allow it. Items marked
     * {@code cost} are paid for with an ordinary item of the same material, which is used up. Nothing is taken or given
     * unless the payment and the new items all fit.
     */
    public Result reclaim(Player p, String id) {
        if (!RewardSpecs.ITEMS.containsKey(id) || !earned(p, id)) return Result.of(Status.NOT_EARNED);
        List<RewardItem> specs = reclaimable(id);
        if (specs.isEmpty()) return Result.of(Status.NOT_RECLAIMABLE);
        long wait = cooldownRemaining(p, id);
        if (wait > 0) return new Result(Status.COOLDOWN, wait, null);

        List<ItemStack> stacks = new ArrayList<>();
        for (RewardItem spec : specs) stacks.addAll(RewardItems.build(id, title(id), spec));
        if (!unlimited(p)) {
            Map<String, Integer> coming = new HashMap<>();
            stacks.forEach(s -> coming.merge(RewardLimit.key(id, s.getType().getKey().toString()), s.getAmount(), Integer::sum));
            if (!RewardLimit.fits(RewardLimit.allowance(id, specs), RewardScan.held(p), coming)) return Result.of(Status.HOLDING);
        }

        // work on a copy of the main inventory; it only replaces the real one if payment and delivery both succeed
        Inventory scratch = Bukkit.createInventory(null, 36);
        ItemStack[] storage = p.getInventory().getStorageContents();
        for (int i = 0; i < storage.length && i < 36; i++) scratch.setItem(i, storage[i] == null ? null : storage[i].clone());
        for (Map.Entry<String, Integer> cost : RewardReclaim.cost(specs).entrySet()) {
            Material material = Material.matchMaterial(cost.getKey());
            if (material == null || !pay(scratch, material, cost.getValue())) {
                return new Result(Status.MISSING_COST, 0, cost.getValue() + "x " + prettyName(cost.getKey()));
            }
        }
        if (!scratch.addItem(stacks.stream().map(ItemStack::clone).toArray(ItemStack[]::new)).isEmpty()) {
            return Result.of(Status.NO_SPACE);
        }
        p.getInventory().setStorageContents(scratch.getContents());
        if (!p.hasPermission(PERM_NO_COOLDOWN)) store.setLong(p, reclaimKey(id), System.currentTimeMillis());
        return Result.of(Status.OK);
    }

    /** Uses up {@code amount} ordinary items of the material from the inventory; false (and no change) if short. */
    private static boolean pay(Inventory inv, Material material, int amount) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack s = inv.getItem(i);
            // a reward copy can't pay for another reward
            if (s != null && s.getType() == material && RewardItems.idOf(s) == null) slots.add(i);
        }
        int available = 0;
        for (int slot : slots) available += inv.getItem(slot).getAmount();
        if (available < amount) return false;

        int[] enchants = new int[slots.size()], damage = new int[slots.size()];
        for (int k = 0; k < slots.size(); k++) {
            ItemStack s = inv.getItem(slots.get(k));
            enchants[k] = s.getEnchantments().size();
            damage[k] = s.getItemMeta() instanceof Damageable d ? d.getDamage() : 0;
        }
        int left = amount;
        for (int k : RewardReclaim.payOrder(enchants, damage)) {
            ItemStack s = inv.getItem(slots.get(k));
            int take = Math.min(left, s.getAmount());
            if (take >= s.getAmount()) inv.setItem(slots.get(k), null);
            else s.setAmount(s.getAmount() - take);
            left -= take;
            if (left == 0) break;
        }
        return true;
    }

    private static String prettyName(String itemId) {
        String raw = itemId.substring(itemId.indexOf(':') + 1).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    /** Admin menu: gives the reward with no checks and no cooldown; what does not fit lands at the player's feet. */
    public void give(Player p, String id) {
        for (ItemStack leftover : p.getInventory().addItem(RewardItems.buildAll(id, title(id)).toArray(ItemStack[]::new)).values()) {
            p.getWorld().dropItem(p.getLocation(), leftover);
        }
    }

    /** True if the player already holds a full set of this reward, for the menu lore. */
    public boolean holdsAll(Player p, String id, Map<String, Integer> held) {
        Map<String, Integer> allowed = RewardLimit.allowance(id, reclaimable(id));
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
}

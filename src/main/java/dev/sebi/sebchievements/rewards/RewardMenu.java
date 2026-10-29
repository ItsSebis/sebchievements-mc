package dev.sebi.sebchievements.rewards;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.RewardSpecs;
import dev.sebi.sebchievements.core.Progress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The reward screen: 45 reward slots plus a navigation row. Everyone sees the rewards of advancements they earned
 * ("My rewards"); admins can toggle to every reward in the catalog and take any of them.
 */
public final class RewardMenu implements InventoryHolder {
    static final int ROWS = 6;
    static final int PAGE_SIZE = 45;
    static final int PREV = 45, TOGGLE = 47, INFO = 49, NEXT = 53;

    private final RewardService service;
    private final Player viewer;
    private boolean showAll;
    private int page;
    private List<String> shown = List.of();
    private Inventory inventory;

    private RewardMenu(RewardService service, Player viewer) {
        this.service = service;
        this.viewer = viewer;
    }

    /** Opens the menu in the player view (admins can switch inside). */
    public static void open(RewardService service, Player viewer) {
        RewardMenu menu = new RewardMenu(service, viewer);
        menu.render();
        viewer.openInventory(menu.inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private boolean isAdmin() {
        return viewer.hasPermission("smpadv.admin");
    }

    // ---- clicks ----

    void click(int slot) {
        if (slot == PREV && page > 0) {
            page--;
            render();
        } else if (slot == NEXT && (page + 1) * PAGE_SIZE < shown.size()) {
            page++;
            render();
        } else if (slot == TOGGLE && isAdmin()) {
            showAll = !showAll;
            page = 0;
            inventory = null; // the title changes with the mode, so the screen is reopened
            render();
            viewer.openInventory(inventory);
        } else if (slot >= 0 && slot < PAGE_SIZE && page * PAGE_SIZE + slot < shown.size()) {
            take(shown.get(page * PAGE_SIZE + slot));
        }
    }

    private void take(String id) {
        if (showAll && isAdmin()) {
            service.give(viewer, id);
            viewer.sendMessage(Component.text("Took the reward of " + service.title(id) + ".", NamedTextColor.GREEN));
            viewer.playSound(viewer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1f);
        } else {
            RewardService.Result r = service.reclaim(viewer, id);
            switch (r.status()) {
                case OK -> {
                    viewer.sendMessage(Component.text("Reclaimed the reward of " + service.title(id) + ".", NamedTextColor.GREEN));
                    viewer.playSound(viewer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1f);
                }
                case NOT_EARNED -> fail("You haven't earned that advancement.");
                case COOLDOWN -> fail("You can reclaim that again in " + Progress.duration(r.waitMillis()) + ".");
                case HOLDING -> fail("You already have that reward.");
                case NO_SPACE -> fail("Not enough room in your inventory.");
            }
        }
        render();
    }

    private void fail(String text) {
        viewer.sendMessage(Component.text(text, NamedTextColor.RED));
        viewer.playSound(viewer.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }

    // ---- drawing ----

    private void render() {
        shown = ids();
        int pages = Math.max(1, (shown.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, pages - 1);
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, ROWS * 9, Component.text(showAll ? "Rewards (admin: all)" : "Rewards"));
        }
        inventory.clear();
        Map<String, Integer> held = showAll ? Map.of() : RewardScan.held(viewer);
        for (int i = 0; i < PAGE_SIZE && page * PAGE_SIZE + i < shown.size(); i++) {
            inventory.setItem(i, cell(shown.get(page * PAGE_SIZE + i), held));
        }
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int s = PAGE_SIZE; s < ROWS * 9; s++) inventory.setItem(s, filler);
        if (page > 0) inventory.setItem(PREV, named(Material.ARROW, "Previous page", List.of()));
        if ((page + 1) * PAGE_SIZE < shown.size()) inventory.setItem(NEXT, named(Material.ARROW, "Next page", List.of()));
        String what = showAll ? "All rewards (admin)" : "Your rewards";
        List<String> infoLore = showAll ? List.of("Clicking gives the item, no limits.")
                : List.of("Click a reward you lost to get it back.", "You can't hold more than one set,",
                        "and each reclaim has a cooldown.");
        inventory.setItem(INFO, named(Material.BOOK, what + " - page " + (page + 1) + "/" + pages, infoLore));
        if (isAdmin()) {
            inventory.setItem(TOGGLE, named(Material.LEVER, showAll ? "Switch to: your rewards" : "Switch to: all rewards (admin)",
                    List.of("Admins can toggle between the player view", "and every reward in the catalog.")));
        }
    }

    /** Advancement ids with a reward, ordered by tab, then difficulty, then id. */
    private List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (String id : RewardSpecs.ITEMS.keySet()) {
            if (showAll || service.earned(viewer, id)) ids.add(id);
        }
        ids.sort(Comparator.<String>comparingInt(id -> AdvancementIds.TABS.indexOf(AdvancementIds.PATHS.get(id).split("/")[0]))
                .thenComparingInt(id -> AdvancementIds.DIFFICULTY.getOrDefault(id, 0))
                .thenComparing(id -> id));
        return ids;
    }

    private ItemStack cell(String id, Map<String, Integer> held) {
        List<ItemStack> stacks = RewardItems.buildAll(id, service.title(id));
        ItemStack icon = stacks.get(0).clone();
        ItemMeta meta = icon.getItemMeta();
        // display copies must never pass for the real thing
        meta.getPersistentDataContainer().remove(RewardItems.TAG);
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.add(Component.empty());
        if (stacks.size() > 1 || RewardSpecs.ITEMS.get(id).size() > 1) {
            lore.add(RewardItems.plain("Contains:", NamedTextColor.GOLD));
            for (RewardItem item : RewardSpecs.ITEMS.get(id)) {
                String name = item.name() != null ? item.name() : Material.matchMaterial(item.item()).name();
                lore.add(RewardItems.plain(" " + item.count() + "x " + name, NamedTextColor.WHITE));
            }
        }
        if (showAll) {
            lore.add(RewardItems.plain("Click to take", NamedTextColor.GREEN));
        } else {
            long wait = service.cooldownRemaining(viewer, id);
            if (wait > 0) lore.add(RewardItems.plain("Available again in " + Progress.duration(wait), NamedTextColor.RED));
            else if (service.holdsAll(viewer, id, held)) lore.add(RewardItems.plain("You already have this", NamedTextColor.YELLOW));
            else lore.add(RewardItems.plain("Click to reclaim", NamedTextColor.GREEN));
        }
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private static ItemStack named(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.customName(RewardItems.plain(name, NamedTextColor.WHITE));
        if (!lore.isEmpty()) meta.lore(lore.stream().map(l -> RewardItems.plain(l, NamedTextColor.GRAY)).toList());
        stack.setItemMeta(meta);
        return stack;
    }
}

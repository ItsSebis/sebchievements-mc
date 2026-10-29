package dev.sebi.sebchievements.rewards;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.PerkSpecs;
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
    static final int PREV = 45, TOGGLE = 47, INFO = 49, PERKS = 51, NEXT = 53;

    /** One menu cell: the reward items of an advancement, or its effect perk. */
    private record Entry(String id, boolean perk) {}

    private final RewardService service;
    private final PerkService perks;
    private final Player viewer;
    private boolean showAll;
    private int page;
    private List<Entry> shown = List.of();
    private Inventory inventory;

    private RewardMenu(RewardService service, PerkService perks, Player viewer) {
        this.service = service;
        this.perks = perks;
        this.viewer = viewer;
    }

    /** Opens the menu in the player view (admins can switch inside). */
    public static void open(RewardService service, PerkService perks, Player viewer) {
        RewardMenu menu = new RewardMenu(service, perks, viewer);
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
        } else if (slot == PERKS) {
            PerkMenu.open(service, perks, viewer);
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

    private void take(Entry entry) {
        String id = entry.id();
        if (entry.perk()) {
            togglePerk(id);
        } else if (showAll && isAdmin()) {
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
                case NOT_RECLAIMABLE -> fail("That reward is used up and can't be reclaimed.");
                case MISSING_COST -> fail("You need " + r.detail() + " in your inventory to reclaim that (it is used up).");
                case COOLDOWN -> fail("You can reclaim that again in " + Progress.duration(r.waitMillis()) + ".");
                case HOLDING -> fail("You already have that reward.");
                case NO_SPACE -> fail("Not enough room in your inventory.");
            }
        }
        render();
    }

    private void togglePerk(String id) {
        PerkMenu.toggle(perks, viewer, id);
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
            Entry entry = shown.get(page * PAGE_SIZE + i);
            inventory.setItem(i, entry.perk() ? PerkMenu.cell(service, perks, viewer, entry.id()) : cell(entry.id(), held));
        }
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int s = PAGE_SIZE; s < ROWS * 9; s++) inventory.setItem(s, filler);
        if (page > 0) inventory.setItem(PREV, named(Material.ARROW, "Previous page", List.of()));
        if ((page + 1) * PAGE_SIZE < shown.size()) inventory.setItem(NEXT, named(Material.ARROW, "Next page", List.of()));
        String what = showAll ? "All rewards (admin)" : "Your rewards";
        List<String> infoLore = showAll ? List.of("Clicking gives the item, no limits.")
                : List.of("Click a reward you lost to get it back.", "You can't hold more than one set,",
                        "and each reclaim has a cooldown.", "Perks are switched on and off with a click.");
        inventory.setItem(INFO, named(Material.BOOK, what + " - page " + (page + 1) + "/" + pages, infoLore));
        inventory.setItem(PERKS, named(Material.BREWING_STAND, "Perks", List.of(perks.summary(viewer),
                "Click to open the perk menu.")));
        if (isAdmin()) {
            inventory.setItem(TOGGLE, named(Material.LEVER, showAll ? "Switch to: your rewards" : "Switch to: all rewards (admin)",
                    List.of("Admins can toggle between the player view", "and every reward in the catalog.")));
        }
    }

    /** Rewards and perks, ordered by tab, then difficulty, then id (items before the perk of the same advancement). */
    private List<Entry> ids() {
        List<Entry> entries = new ArrayList<>();
        for (String id : RewardSpecs.ITEMS.keySet()) {
            if (showAll || service.earned(viewer, id) && !RewardService.reclaimable(id).isEmpty()) entries.add(new Entry(id, false));
        }
        for (String id : PerkSpecs.PERKS.keySet()) {
            if (showAll || service.earned(viewer, id)) entries.add(new Entry(id, true));
        }
        entries.sort(Comparator.<Entry>comparingInt(e -> AdvancementIds.TABS.indexOf(AdvancementIds.PATHS.get(e.id()).split("/")[0]))
                .thenComparingInt(e -> AdvancementIds.DIFFICULTY.getOrDefault(e.id(), 0))
                .thenComparing(Entry::id)
                .thenComparing(Entry::perk));
        return entries;
    }

    private ItemStack cell(String id, Map<String, Integer> held) {
        List<RewardItem> all = RewardSpecs.ITEMS.get(id);
        List<RewardItem> given = showAll ? all : RewardService.reclaimable(id);
        ItemStack icon = RewardItems.build(id, service.title(id), given.get(0)).get(0);
        ItemMeta meta = icon.getItemMeta();
        // display copies must never pass for the real thing
        meta.getPersistentDataContainer().remove(RewardItems.TAG);
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.add(Component.empty());
        boolean notes = !showAll && all.stream().anyMatch(i -> !RewardReclaim.FREE.equals(i.reclaim()));
        if (all.size() > 1 || notes) {
            lore.add(RewardItems.plain("Contains:", NamedTextColor.GOLD));
            for (RewardItem item : all) {
                String name = item.name() != null ? item.name() : Material.matchMaterial(item.item()).name();
                String note = "";
                if (!showAll && RewardReclaim.NEVER.equals(item.reclaim())) note = " - used up, can't be reclaimed";
                else if (!showAll && RewardReclaim.COST.equals(item.reclaim())) note = " - costs " + item.count() + "x " + plainMaterial(item.item());
                boolean never = !showAll && RewardReclaim.NEVER.equals(item.reclaim());
                lore.add(RewardItems.plain(" " + item.count() + "x " + name + note, never ? NamedTextColor.DARK_GRAY : NamedTextColor.WHITE));
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

    private static String plainMaterial(String itemId) {
        return itemId.substring(itemId.indexOf(':') + 1).replace('_', ' ') + " (used up)";
    }

    static ItemStack named(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.customName(RewardItems.plain(name, NamedTextColor.WHITE));
        if (!lore.isEmpty()) meta.lore(lore.stream().map(l -> RewardItems.plain(l, NamedTextColor.GRAY)).toList());
        stack.setItemMeta(meta);
        return stack;
    }
}

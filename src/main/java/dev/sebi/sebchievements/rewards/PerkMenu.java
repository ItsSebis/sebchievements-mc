package dev.sebi.sebchievements.rewards;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.PerkSpecs;
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

/**
 * {@code /adv perks}: every effect perk with its state, one click to switch it on or off. Locked perks are listed too
 * (greyed out), so players can see what there is to earn.
 */
public final class PerkMenu implements InventoryHolder {
    private static final int ROWS = 6;
    private static final int MAX_PERKS = 45;
    private static final int REWARDS = 45, INFO = 49;

    private final RewardService service;
    private final PerkService perks;
    private final Player viewer;
    private List<String> shown = List.of();
    private Inventory inventory;

    private PerkMenu(RewardService service, PerkService perks, Player viewer) {
        this.service = service;
        this.perks = perks;
        this.viewer = viewer;
    }

    public static void open(RewardService service, PerkService perks, Player viewer) {
        PerkMenu menu = new PerkMenu(service, perks, viewer);
        menu.inventory = Bukkit.createInventory(menu, ROWS * 9, Component.text("Perks"));
        menu.render();
        viewer.openInventory(menu.inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void click(int slot) {
        if (slot == REWARDS) {
            RewardMenu.open(service, perks, viewer);
        } else if (slot >= 0 && slot < shown.size()) {
            toggle(perks, viewer, shown.get(slot));
            render();
        }
    }

    private void render() {
        inventory.clear();
        // unlocked first, then by tab and difficulty
        shown = new ArrayList<>(PerkSpecs.PERKS.keySet());
        shown.sort(Comparator.<String>comparingInt(id -> perks.earned(viewer, id) ? 0 : 1)
                .thenComparingInt(id -> AdvancementIds.TABS.indexOf(AdvancementIds.PATHS.get(id).split("/")[0]))
                .thenComparingInt(id -> AdvancementIds.DIFFICULTY.getOrDefault(id, 0))
                .thenComparing(id -> id));
        if (shown.size() > MAX_PERKS) shown = shown.subList(0, MAX_PERKS);
        for (int i = 0; i < shown.size(); i++) inventory.setItem(i, cell(service, perks, viewer, shown.get(i)));
        ItemStack filler = RewardMenu.named(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int s = MAX_PERKS; s < ROWS * 9; s++) inventory.setItem(s, filler);
        inventory.setItem(REWARDS, RewardMenu.named(Material.CHEST, "Back to rewards", List.of()));
        inventory.setItem(INFO, RewardMenu.named(Material.BREWING_STAND, "Your perks", List.of(perks.summary(viewer),
                "Click a perk to switch it on or off.", "Switched-off perks fade within a second or two.")));
    }

    /** One perk as a menu item; shared with the rewards menu. */
    static ItemStack cell(RewardService service, PerkService perks, Player viewer, String id) {
        PerkSpec spec = PerkSpecs.PERKS.get(id);
        Material icon = Material.matchMaterial(spec.icon());
        boolean earned = perks.earned(viewer, id);
        boolean on = earned && perks.enabled(viewer, id);
        List<String> lore = new ArrayList<>();
        lore.add("Perk: " + effectName(spec));
        lore.add("From: " + service.title(id));
        lore.add("");
        lore.add(!earned ? "Not earned yet" : on ? "ENABLED - click to switch off" : "DISABLED - click to switch on");
        ItemStack stack = RewardMenu.named(!earned ? Material.GRAY_DYE : icon == null ? Material.POTION : icon, spec.name(), lore);
        if (on) {
            ItemMeta meta = stack.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** Switches a perk and tells the player; shared with the rewards menu. */
    static void toggle(PerkService perks, Player viewer, String id) {
        PerkSpec spec = PerkSpecs.PERKS.get(id);
        if (!perks.earned(viewer, id)) {
            viewer.sendMessage(Component.text("You haven't earned " + spec.name() + " yet.", NamedTextColor.RED));
            viewer.playSound(viewer.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            return;
        }
        boolean on = perks.toggle(viewer, id);
        viewer.sendMessage(Component.text(spec.name() + (on ? " enabled." : " disabled. It fades within a second or two."),
                on ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, on ? 1.2f : 0.8f);
    }

    static String effectName(PerkSpec spec) {
        String raw = spec.effect().substring(spec.effect().indexOf(':') + 1).replace('_', ' ');
        String[] roman = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1) + " " + roman[Math.min(spec.amplifier(), 9)];
    }
}

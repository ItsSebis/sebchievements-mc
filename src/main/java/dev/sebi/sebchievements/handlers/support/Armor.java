package dev.sebi.sebchievements.handlers.support;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class Armor {
    private Armor() {}

    /** True if any of the four armor slots holds an item (an elytra or a pumpkin on the head counts too). */
    public static boolean wearsAny(Player p) {
        PlayerInventory inv = p.getInventory();
        for (ItemStack piece : new ItemStack[] {inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()}) {
            if (piece != null && piece.getType() != Material.AIR) return true;
        }
        return false;
    }
}

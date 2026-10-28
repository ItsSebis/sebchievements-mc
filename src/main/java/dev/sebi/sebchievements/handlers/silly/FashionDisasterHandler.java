package dev.sebi.sebchievements.handlers.silly;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** All four armor slots filled with pieces of at least N different materials (leather, iron, ...). */
public final class FashionDisasterHandler implements AdvancementHandler, Listener {
    private static final Set<String> PIECES = Set.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");

    private HandlerContext ctx;
    private int needed;

    @Override
    public String id() {
        return AdvancementIds.FASHION_DISASTER;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.needed = ctx.config().getInt("distinct_materials", 4);
        ctx.listen(this);
    }

    /** {@code IRON_HELMET -> IRON}; empty for anything that is not a helmet/chestplate/leggings/boots (e.g. elytra). */
    public static Optional<String> family(String materialName) {
        int i = materialName.lastIndexOf('_');
        if (i < 0 || !PIECES.contains(materialName.substring(i + 1))) return Optional.empty();
        return Optional.of(materialName.substring(0, i));
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent e) {
        Player p = e.getPlayer();
        PlayerInventory inv = p.getInventory();
        Set<String> families = new HashSet<>();
        for (ItemStack piece : new ItemStack[] {inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()}) {
            if (piece == null) return;
            Optional<String> f = family(piece.getType().name());
            if (f.isEmpty()) return;
            families.add(f.get());
        }
        if (families.size() >= needed) ctx.grant(p);
    }
}

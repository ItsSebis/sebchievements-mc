package dev.sebi.sebchievements.handlers.social;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.Optional;

/** Both wear all four armor slots with the exact same item types as a player within the radius. */
public final class TwinningHandler implements AdvancementHandler {
    @Override
    public String id() {
        return AdvancementIds.TWINNING;
    }

    @Override
    public void register(HandlerContext ctx) {
        double radius = ctx.config().getDouble("radius", 8);
        ctx.services().tick().register(ctx.id(), p -> {
            Optional<Material[]> mine = armor(p);
            if (mine.isEmpty()) return;
            for (Player other : ctx.services().proximity().nearby(p, radius)) {
                Optional<Material[]> theirs = armor(other);
                if (theirs.isPresent() && Arrays.equals(mine.get(), theirs.get())) {
                    ctx.grant(p);
                    return;
                }
            }
        });
    }

    /** Helmet, chestplate, leggings, boots types, or empty if any slot is empty. */
    private static Optional<Material[]> armor(Player p) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] pieces = {inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()};
        Material[] types = new Material[4];
        for (int i = 0; i < 4; i++) {
            if (pieces[i] == null || pieces[i].getType().isAir()) return Optional.empty();
            types[i] = pieces[i].getType();
        }
        return Optional.of(types);
    }
}

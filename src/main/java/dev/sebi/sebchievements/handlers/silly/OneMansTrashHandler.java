package dev.sebi.sebchievements.handlers.silly;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.core.Progress;
import dev.sebi.sebchievements.core.ProgressReporting;
import org.bukkit.GameMode;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** A streak of junk catches; a fish or treasure catch (including an enchanted fishing rod) resets it. */
public final class OneMansTrashHandler implements AdvancementHandler, ProgressReporting, Listener {
    private HandlerContext ctx;
    private long target;
    private final Set<String> junk = new HashSet<>();

    @Override
    public String id() {
        return AdvancementIds.ONE_MANS_TRASH;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.target = ctx.config().getLong("count", 10);
        junk.addAll(ctx.config().getStringList("junk"));
        ctx.listen(this);
    }

    /** Junk = listed in config, except an enchanted item (an enchanted fishing rod is treasure). */
    public static boolean isJunk(String materialName, boolean enchanted, Set<String> junkList) {
        return junkList.contains(materialName) && !enchanted;
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(e.getCaught() instanceof Item caught)) return;
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (ctx.services().afk().isAfk(p)) return; // AFK catches neither count nor reset
        ItemStack stack = caught.getItemStack();
        if (isJunk(stack.getType().name(), stack.getItemMeta() != null && stack.getItemMeta().hasEnchants(), junk)) {
            if (ctx.services().streaks().increment(p.getUniqueId(), ctx.id()) >= target) {
                ctx.grant(p);
                ctx.services().streaks().reset(p.getUniqueId(), ctx.id());
            }
        } else {
            ctx.services().streaks().reset(p.getUniqueId(), ctx.id());
        }
    }

    @Override
    public Optional<Progress> progress(Player player) {
        return Optional.of(Progress.count(ctx.services().streaks().get(player.getUniqueId(), ctx.id()), target));
    }

    @Override
    public List<String> debug(Player player) {
        return List.of("streak = " + ctx.services().streaks().get(player.getUniqueId(), ctx.id()) + " / " + target);
    }
}

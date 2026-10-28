package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.core.Progress;
import dev.sebi.sebchievements.core.ProgressReporting;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** All listed foods within the window (WindowService), then survive N seconds; death or logout cancels. */
public final class FoodCriticHandler implements AdvancementHandler, ProgressReporting, Listener {
    private final Set<String> foods = new HashSet<>();
    private final Map<UUID, Long> surviveUntil = new HashMap<>();
    private HandlerContext ctx;
    private long windowMillis, surviveMillis;

    @Override
    public String id() {
        return AdvancementIds.FOOD_CRITIC;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        foods.addAll(ctx.config().getStringList("foods"));
        windowMillis = ctx.config().getLong("window_seconds", 60) * 1000;
        surviveMillis = ctx.config().getLong("survive_seconds", 30) * 1000;
        ctx.services().tick().register(ctx.id(), this::check);
        ctx.listen(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        String food = e.getItem().getType().name();
        if (!foods.contains(food) || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        long now = System.currentTimeMillis();
        if (ctx.services().windows().record(p.getUniqueId(), ctx.id(), food, now, windowMillis) >= foods.size()) {
            surviveUntil.putIfAbsent(p.getUniqueId(), now + surviveMillis);
        }
    }

    private void check(Player p) {
        Long due = surviveUntil.get(p.getUniqueId());
        if (due == null || System.currentTimeMillis() < due) return;
        surviveUntil.remove(p.getUniqueId());
        ctx.services().windows().clear(p.getUniqueId(), ctx.id());
        if (!p.isDead()) ctx.grant(p);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        cancel(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cancel(e.getPlayer().getUniqueId());
    }

    private void cancel(UUID id) {
        surviveUntil.remove(id);
        ctx.services().windows().clear(id, ctx.id());
    }

    @Override
    public Optional<Progress> progress(Player p) {
        long now = System.currentTimeMillis();
        Long due = surviveUntil.get(p.getUniqueId());
        if (due != null) return Optional.of(Progress.time(surviveMillis - Math.max(0, due - now), surviveMillis));
        return Optional.of(Progress.count(ctx.services().windows().count(p.getUniqueId(), ctx.id(), now, windowMillis), foods.size()));
    }
}

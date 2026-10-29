package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.services.RecentDamageService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;

/** A goat that dies in the End void goes to the player who hit it last within the window. */
public final class YeetTheGoatHandler implements AdvancementHandler, Listener {
    private HandlerContext ctx;
    private long windowMillis;

    @Override
    public String id() {
        return AdvancementIds.YEET_THE_GOAT;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.windowMillis = ctx.config().getLong("window_seconds", 10) * 1000;
        ctx.listen(this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Goat goat) || goat.getWorld().getEnvironment() != World.Environment.THE_END) return;
        if (!(goat.getLastDamageCause() instanceof EntityDamageEvent last) || last.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        List<RecentDamageService.Hit> hits = ctx.services().damage().recent(goat.getUniqueId(), windowMillis, System.currentTimeMillis());
        for (int i = hits.size() - 1; i >= 0; i--) {
            Player pusher = Bukkit.getPlayer(hits.get(i).damager());
            if (pusher == null || !pusher.getWorld().equals(goat.getWorld())) continue;
            if (pusher.getGameMode() == GameMode.CREATIVE || pusher.getGameMode() == GameMode.SPECTATOR) continue;
            ctx.grant(pusher);
            return;
        }
    }
}

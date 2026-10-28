package dev.sebi.sebchievements.handlers.challenges;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.HandlerContext;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Raid;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Tracked from the moment the raid starts: any other player (spectators excluded) within the radius of the raid
 * centre at any check disqualifies it. The raid must end in VICTORY at the configured Bad Omen level, with the
 * player who triggered it among the winners.
 */
public final class OneManArmyHandler implements AdvancementHandler, Listener {
    private static final class Tracked {
        final Raid raid;
        final UUID player;
        boolean disqualified;

        Tracked(Raid raid, UUID player) {
            this.raid = raid;
            this.player = player;
        }
    }

    private final Map<UUID, Tracked> raids = new HashMap<>();
    private HandlerContext ctx;
    private NamespacedKey tagKey;

    @Override
    public String id() {
        return AdvancementIds.ONE_MAN_ARMY;
    }

    @Override
    public void register(HandlerContext ctx) {
        this.ctx = ctx;
        this.tagKey = new NamespacedKey(ctx.plugin(), "one_man_army_raid");
        ctx.services().tick().registerGlobal(this::checkRaids);
        ctx.listen(this);
    }

    private void checkRaids() {
        for (Iterator<Tracked> it = raids.values().iterator(); it.hasNext();) {
            Tracked t = it.next();
            if (t.raid.getStatus() != Raid.RaidStatus.ONGOING) {
                it.remove();
                continue;
            }
            scan(t);
        }
    }

    /** Disqualifies the raid if someone other than the owner stands within the radius of its centre. */
    private void scan(Tracked t) {
        Location center = t.raid.getLocation();
        double r = ctx.config().getDouble("radius", 64);
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getUniqueId().equals(t.player) || p.getGameMode() == GameMode.SPECTATOR) continue;
            if (p.getLocation().distanceSquared(center) <= r * r) {
                t.disqualified = true;
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrigger(RaidTriggerEvent e) {
        // Raid#getId is deprecated for removal, so identify the raid by a tag of our own in its PDC
        UUID tag = UUID.randomUUID();
        e.getRaid().getPersistentDataContainer().set(tagKey, PersistentDataType.STRING, tag.toString());
        Tracked t = new Tracked(e.getRaid(), e.getPlayer().getUniqueId());
        raids.put(tag, t);
        scan(t);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFinish(RaidFinishEvent e) {
        String tag = e.getRaid().getPersistentDataContainer().get(tagKey, PersistentDataType.STRING);
        Tracked t = tag == null ? null : raids.remove(UUID.fromString(tag));
        if (t == null) return;
        scan(t);
        if (t.disqualified || e.getRaid().getStatus() != Raid.RaidStatus.VICTORY
                || e.getRaid().getBadOmenLevel() < ctx.config().getInt("omen_level", 5)) return;
        Player owner = Bukkit.getPlayer(t.player);
        if (owner != null && e.getWinners().contains(owner)) ctx.grant(owner);
    }
}

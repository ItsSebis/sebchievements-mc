package dev.sebi.sebchievements.core;

import dev.sebi.sebchievements.AdvancementIds;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/** The single place advancements are awarded or revoked. */
public final class Granter implements Listener {
    public static final String DONE = "done";

    private final Plugin plugin;
    private final Logger log;
    /** Grants requested while the player was dead, run on respawn so the reward is not lost with the death drops. */
    private final Map<UUID, List<Runnable>> afterRespawn = new HashMap<>();

    public Granter(Plugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        List<Runnable> pending = afterRespawn.remove(p.getUniqueId());
        if (pending != null) plugin.getServer().getScheduler().runTask(plugin, () -> pending.forEach(Runnable::run));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        afterRespawn.remove(e.getPlayer().getUniqueId());
    }

    /** Awards {@code done}. Applies the creative/spectator rule for timed and streak advancements. */
    public boolean grant(Player player, String id) {
        return grantCriterion(player, id, DONE);
    }

    public boolean grantCriterion(Player player, String id, String criterion) {
        if (AdvancementIds.TRACKED.contains(id) && isCreativeOrSpectator(player)) {
            return false;
        }
        if (player.isDead()) {
            afterRespawn.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                    .add(() -> grantCriterion(player, id, criterion));
            return true;
        }
        return award(player, id, criterion);
    }

    /** Admin path: awards every remaining criterion, ignoring game mode. */
    public boolean forceGrant(Player player, String id) {
        AdvancementProgress progress = progress(player, id);
        if (progress == null || progress.isDone()) return false;
        progress.getRemainingCriteria().forEach(progress::awardCriteria);
        log.info("Granted " + id + " to " + player.getName() + " (admin)");
        return true;
    }

    public boolean revoke(Player player, String id) {
        AdvancementProgress progress = progress(player, id);
        if (progress == null || progress.getAwardedCriteria().isEmpty()) return false;
        progress.getAwardedCriteria().forEach(progress::revokeCriteria);
        log.info("Revoked " + id + " from " + player.getName());
        return true;
    }

    /** Silently gives the player every tab root so the tabs appear. */
    public void grantRoots(Player player) {
        for (String tab : AdvancementIds.TABS) {
            Advancement root = Bukkit.getAdvancement(AdvancementIds.rootKey(tab));
            if (root == null) continue;
            AdvancementProgress progress = player.getAdvancementProgress(root);
            progress.getRemainingCriteria().forEach(progress::awardCriteria);
        }
    }

    private boolean award(Player player, String id, String criterion) {
        AdvancementProgress progress = progress(player, id);
        if (progress == null || progress.isDone() || progress.getAwardedCriteria().contains(criterion)) return false;
        if (!progress.awardCriteria(criterion)) {
            log.warning("Advancement " + id + " has no criterion '" + criterion + "'");
            return false;
        }
        log.info("Granted " + id + (progress.isDone() ? "" : " [" + criterion + "]") + " to " + player.getName());
        return true;
    }

    private AdvancementProgress progress(Player player, String id) {
        Advancement adv = Bukkit.getAdvancement(AdvancementIds.key(id));
        if (adv == null) {
            log.warning("Advancement not loaded: " + id);
            return null;
        }
        return player.getAdvancementProgress(adv);
    }

    private static boolean isCreativeOrSpectator(Player p) {
        return p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR;
    }
}

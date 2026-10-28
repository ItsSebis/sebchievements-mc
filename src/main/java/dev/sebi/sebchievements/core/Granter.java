package dev.sebi.sebchievements.core;

import dev.sebi.sebchievements.AdvancementIds;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/** The single place advancements are awarded or revoked. */
public final class Granter {
    public static final String DONE = "done";

    private final Logger log;

    public Granter(Logger log) {
        this.log = log;
    }

    /** Awards {@code done}. Applies the creative/spectator rule for timed and streak advancements. */
    public boolean grant(Player player, String id) {
        return grantCriterion(player, id, DONE);
    }

    public boolean grantCriterion(Player player, String id, String criterion) {
        if (AdvancementIds.TRACKED.contains(id) && isCreativeOrSpectator(player)) {
            return false;
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

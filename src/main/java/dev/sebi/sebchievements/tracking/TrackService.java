package dev.sebi.sebchievements.tracking;

import dev.sebi.sebchievements.AdvancementIds;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.PlayerDataStore;
import dev.sebi.sebchievements.core.Progress;
import dev.sebi.sebchievements.core.ProgressReporting;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * {@code /adv track}: one boss bar per tracked advancement, shown only to its player and refreshed from the shared
 * {@code TickScheduler}. Tracked ids are kept in the player's data so the bars come back after a relog.
 */
public final class TrackService implements Listener {
    private static final String KEY = "track/ids";

    public enum Result { TRACKED, ALREADY, UNKNOWN, DONE, NO_PROGRESS }

    private final List<AdvancementHandler> handlers;
    private final PlayerDataStore store;
    private final Map<UUID, Map<String, BossBar>> bars = new HashMap<>();

    public TrackService(List<AdvancementHandler> handlers, PlayerDataStore store) {
        this.handlers = handlers;
        this.store = store;
    }

    /** Bar fill for a progress value: 0 to 1, and 0 when there is no target. */
    static float fill(Progress p) {
        if (p.target() <= 0) return 0f;
        return (float) Math.max(0.0, Math.min(1.0, (double) p.current() / p.target()));
    }

    static String encode(Set<String> ids) {
        return String.join(",", ids);
    }

    /** Parses the stored list, dropping blanks, duplicates and ids that no longer exist. */
    static Set<String> decode(String raw, Set<String> valid) {
        if (raw == null || raw.isEmpty()) return new LinkedHashSet<>();
        return Arrays.stream(raw.split(",")).filter(valid::contains).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** The ids the player is tracking, in the order they were added. */
    public Set<String> tracked(Player p) {
        Map<String, BossBar> own = bars.get(p.getUniqueId());
        return own == null ? Set.of() : new LinkedHashSet<>(own.keySet());
    }

    public static String titleOf(Advancement adv) {
        return PlainTextComponentSerializer.plainText().serialize(adv.getDisplay().title());
    }

    public Result track(Player p, String id) {
        Advancement adv = Bukkit.getAdvancement(AdvancementIds.key(id));
        if (adv == null || adv.getDisplay() == null || !AdvancementIds.ALL.contains(id)) return Result.UNKNOWN;
        if (bars.getOrDefault(p.getUniqueId(), Map.of()).containsKey(id)) return Result.ALREADY;
        if (p.getAdvancementProgress(adv).isDone()) return Result.DONE;
        if (!hasProgress(id, adv)) return Result.NO_PROGRESS;
        show(p, id, adv, read(p, id, adv));
        save(p);
        return Result.TRACKED;
    }

    public boolean untrack(Player p, String id) {
        Map<String, BossBar> own = bars.get(p.getUniqueId());
        BossBar bar = own == null ? null : own.remove(id);
        if (bar == null) return false;
        p.hideBossBar(bar);
        save(p);
        return true;
    }

    public int untrackAll(Player p) {
        Map<String, BossBar> own = bars.remove(p.getUniqueId());
        if (own == null) return 0;
        own.values().forEach(p::hideBossBar);
        store.remove(p, KEY);
        return own.size();
    }

    /** Runs from the shared TickScheduler: refreshes every bar and drops the ones whose advancement is done. */
    public void update(Player p) {
        Map<String, BossBar> own = bars.get(p.getUniqueId());
        if (own == null || own.isEmpty()) return;
        for (String id : List.copyOf(own.keySet())) {
            Advancement adv = Bukkit.getAdvancement(AdvancementIds.key(id));
            if (adv == null || p.getAdvancementProgress(adv).isDone()) {
                untrack(p, id);
                if (adv != null) {
                    p.sendMessage(Component.text("Tracking stopped, " + titleOf(adv) + " is done.", NamedTextColor.GREEN));
                }
                continue;
            }
            render(own.get(id), adv, read(p, id, adv));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        for (String id : decode(store.getString(p, KEY, ""), AdvancementIds.ALL)) track(p, id);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // the stored list stays; only the in-memory bars go
        Map<String, BossBar> own = bars.remove(e.getPlayer().getUniqueId());
        if (own != null) own.values().forEach(e.getPlayer()::hideBossBar);
    }

    private void show(Player p, String id, Advancement adv, Optional<Progress> prog) {
        BossBar bar = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        render(bar, adv, prog);
        bars.computeIfAbsent(p.getUniqueId(), u -> new LinkedHashMap<>()).put(id, bar);
        p.showBossBar(bar);
    }

    private static void render(BossBar bar, Advancement adv, Optional<Progress> prog) {
        float f = prog.map(TrackService::fill).orElse(0f);
        bar.name(Component.text(titleOf(adv) + "  ", NamedTextColor.YELLOW)
                .append(Component.text(prog.map(Progress::format).orElse("not started"), NamedTextColor.WHITE)));
        bar.progress(f);
        bar.color(f >= 1f ? BossBar.Color.GREEN : BossBar.Color.YELLOW);
    }

    private void save(Player p) {
        Map<String, BossBar> own = bars.get(p.getUniqueId());
        if (own == null || own.isEmpty()) store.remove(p, KEY);
        else store.setString(p, KEY, encode(own.keySet()));
    }

    /** A plugin handler's own progress, or the share of criteria already awarded for a multi-criteria advancement. */
    private Optional<Progress> read(Player p, String id, Advancement adv) {
        for (AdvancementHandler h : handlers) {
            if (h.id().equals(id) && h instanceof ProgressReporting r) return r.progress(p);
        }
        AdvancementProgress ap = p.getAdvancementProgress(adv);
        int total = ap.getAwardedCriteria().size() + ap.getRemainingCriteria().size();
        return total > 1 ? Optional.of(Progress.count(ap.getAwardedCriteria().size(), total)) : Optional.empty();
    }

    private boolean hasProgress(String id, Advancement adv) {
        for (AdvancementHandler h : handlers) if (h.id().equals(id) && h instanceof ProgressReporting) return true;
        return adv.getCriteria().size() > 1;
    }
}

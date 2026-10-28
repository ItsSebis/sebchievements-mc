package dev.sebi.sebchievements.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Who or what damaged whom, and when. Entities are identified by uuid (players and mobs alike). */
public final class RecentDamageService {
    public record Hit(UUID damager, double amount, long at) {}

    private static final int MAX_HITS_PER_VICTIM = 32;

    private final long retentionMillis;
    private final Map<UUID, List<Hit>> hits = new HashMap<>();

    public RecentDamageService(long retentionMillis) {
        this.retentionMillis = retentionMillis;
    }

    public void record(UUID victim, UUID damager, double amount, long now) {
        List<Hit> list = hits.computeIfAbsent(victim, x -> new ArrayList<>());
        list.add(new Hit(damager, amount, now));
        if (list.size() > MAX_HITS_PER_VICTIM) list.remove(0);
    }

    /** Hits on {@code victim} no older than {@code withinMillis}, oldest first. */
    public List<Hit> recent(UUID victim, long withinMillis, long now) {
        List<Hit> list = hits.get(victim);
        if (list == null) return List.of();
        return list.stream().filter(h -> now - h.at() <= withinMillis).toList();
    }

    public boolean wasDamagedBy(UUID victim, UUID damager, long withinMillis, long now) {
        return recent(victim, withinMillis, now).stream().anyMatch(h -> h.damager().equals(damager));
    }

    /** Victims who took damage from {@code damager} within the window. */
    public List<UUID> victimsOf(UUID damager, long withinMillis, long now) {
        List<UUID> out = new ArrayList<>();
        hits.forEach((victim, list) -> {
            if (list.stream().anyMatch(h -> h.damager().equals(damager) && now - h.at() <= withinMillis)) out.add(victim);
        });
        return out;
    }

    public void forget(UUID victim) {
        hits.remove(victim);
    }

    /** Drops everything past retention. Run periodically so dead mobs don't pile up. */
    public void purge(long now) {
        for (Iterator<List<Hit>> it = hits.values().iterator(); it.hasNext();) {
            List<Hit> list = it.next();
            list.removeIf(h -> now - h.at() > retentionMillis);
            if (list.isEmpty()) it.remove();
        }
    }
}

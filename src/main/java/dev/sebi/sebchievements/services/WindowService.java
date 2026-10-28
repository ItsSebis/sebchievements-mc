package dev.sebi.sebchievements.services;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "N distinct events within T". In memory only: windows are short, and losing one on restart cannot
 * lock anyone out. Events are distinct by their key (e.g. the food type); repeats refresh the timestamp.
 */
public final class WindowService {
    private record Event(String key, long at) {}

    private final Map<String, Deque<Event>> windows = new HashMap<>();

    private static String k(UUID p, String id) {
        return p + "|" + id;
    }

    /** Records an event and returns how many distinct keys fall in the window ending at {@code now}. */
    public int record(UUID player, String id, String eventKey, long now, long windowMillis) {
        Deque<Event> q = windows.computeIfAbsent(k(player, id), x -> new ArrayDeque<>());
        q.addLast(new Event(eventKey, now));
        return distinct(q, now, windowMillis);
    }

    public int count(UUID player, String id, long now, long windowMillis) {
        Deque<Event> q = windows.get(k(player, id));
        return q == null ? 0 : distinct(q, now, windowMillis);
    }

    /** The distinct event keys inside the window ending at {@code now} (e.g. who ate from the same cake). */
    public Set<String> keys(UUID player, String id, long now, long windowMillis) {
        Deque<Event> q = windows.get(k(player, id));
        if (q == null) return Set.of();
        distinct(q, now, windowMillis);
        Set<String> out = new HashSet<>();
        for (Event e : q) out.add(e.key());
        return out;
    }

    public void clear(UUID player, String id) {
        windows.remove(k(player, id));
    }

    private static int distinct(Deque<Event> q, long now, long windowMillis) {
        while (!q.isEmpty() && now - q.peekFirst().at() > windowMillis) q.removeFirst();
        Set<String> keys = new HashSet<>();
        for (Event e : q) keys.add(e.key());
        return keys.size();
    }
}

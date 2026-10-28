package dev.sebi.sebchievements.core;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The only way per-player state is persisted: the player's PersistentDataContainer, under our namespace.
 * Keys look like {@code timer/untouchable}, i.e. {@code <kind>/<advancement id>} (lowercase, [a-z0-9/._-]).
 */
public final class PlayerDataStore {
    private final String namespace;

    public PlayerDataStore(String namespace) {
        this.namespace = namespace;
    }

    private NamespacedKey k(String key) {
        return new NamespacedKey(namespace, key);
    }

    public long getLong(Player p, String key, long def) {
        return p.getPersistentDataContainer().getOrDefault(k(key), PersistentDataType.LONG, def);
    }

    public void setLong(Player p, String key, long value) {
        p.getPersistentDataContainer().set(k(key), PersistentDataType.LONG, value);
    }

    public long addLong(Player p, String key, long delta) {
        long v = getLong(p, key, 0) + delta;
        setLong(p, key, v);
        return v;
    }

    public String getString(Player p, String key, String def) {
        return p.getPersistentDataContainer().getOrDefault(k(key), PersistentDataType.STRING, def);
    }

    public void setString(Player p, String key, String value) {
        p.getPersistentDataContainer().set(k(key), PersistentDataType.STRING, value);
    }

    public Set<UUID> getUuidSet(Player p, String key) {
        String raw = p.getPersistentDataContainer().get(k(key), PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) return new LinkedHashSet<>();
        return Arrays.stream(raw.split(",")).map(UUID::fromString).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** @return true if the uuid was not in the set before */
    public boolean addToUuidSet(Player p, String key, UUID id) {
        Set<UUID> set = getUuidSet(p, key);
        if (!set.add(id)) return false;
        setString(p, key, set.stream().map(UUID::toString).collect(Collectors.joining(",")));
        return true;
    }

    public void remove(Player p, String key) {
        p.getPersistentDataContainer().remove(k(key));
    }

    /** All stored keys ending in {@code /<id>}, with their raw values, for {@code /adv debug}. */
    public java.util.Map<String, String> dump(Player p, String id) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        java.util.Map<String, String> out = new java.util.TreeMap<>();
        for (NamespacedKey key : pdc.getKeys()) {
            if (!key.getNamespace().equals(namespace) || !key.getKey().endsWith("/" + id)) continue;
            Object value = null;
            for (PersistentDataType<?, ?> type : new PersistentDataType<?, ?>[] {PersistentDataType.LONG, PersistentDataType.STRING}) {
                if (pdc.has(key, type)) {
                    value = pdc.get(key, type);
                    break;
                }
            }
            out.put(key.getKey(), String.valueOf(value));
        }
        return out;
    }
}

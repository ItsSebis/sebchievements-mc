package dev.sebi.sebchievements.services;

import dev.sebi.sebchievements.core.PlayerDataStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/** {@link LongStore} backed by {@link PlayerDataStore}. Offline players read as default and ignore writes. */
public final class PdcLongStore implements LongStore {
    private final PlayerDataStore store;

    public PdcLongStore(PlayerDataStore store) {
        this.store = store;
    }

    @Override
    public long get(UUID player, String key, long def) {
        Player p = Bukkit.getPlayer(player);
        return p == null ? def : store.getLong(p, key, def);
    }

    @Override
    public void set(UUID player, String key, long value) {
        Player p = Bukkit.getPlayer(player);
        if (p != null) store.setLong(p, key, value);
    }

    @Override
    public void remove(UUID player, String key) {
        Player p = Bukkit.getPlayer(player);
        if (p != null) store.remove(p, key);
    }
}

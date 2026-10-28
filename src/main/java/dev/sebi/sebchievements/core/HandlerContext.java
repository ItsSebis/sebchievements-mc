package dev.sebi.sebchievements.core;

import dev.sebi.sebchievements.services.Services;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/** Everything a handler needs; scoped to the handler's advancement id. */
public record HandlerContext(Plugin plugin, String id, Granter granter, PlayerDataStore store,
                             Services services, ConfigurationSection config) {

    public boolean grant(Player player) {
        return granter.grant(player, id);
    }

    public boolean grantCriterion(Player player, String criterion) {
        return granter.grantCriterion(player, id, criterion);
    }

    public void listen(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }
}

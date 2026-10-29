package dev.sebi.sebchievements;

import dev.sebi.sebchievements.command.AdvCommand;
import dev.sebi.sebchievements.core.AdvancementHandler;
import dev.sebi.sebchievements.core.Granter;
import dev.sebi.sebchievements.core.HandlerContext;
import dev.sebi.sebchievements.core.HandlerValidator;
import dev.sebi.sebchievements.core.Handlers;
import dev.sebi.sebchievements.core.PlayerDataStore;
import dev.sebi.sebchievements.rewards.RewardGuard;
import dev.sebi.sebchievements.rewards.RewardMenuListener;
import dev.sebi.sebchievements.rewards.RewardService;
import dev.sebi.sebchievements.services.AfkService;
import dev.sebi.sebchievements.services.CounterService;
import dev.sebi.sebchievements.services.PdcLongStore;
import dev.sebi.sebchievements.services.ProximityService;
import dev.sebi.sebchievements.services.RecentDamageListener;
import dev.sebi.sebchievements.services.RecentDamageService;
import dev.sebi.sebchievements.services.Services;
import dev.sebi.sebchievements.services.StreakService;
import dev.sebi.sebchievements.services.TickScheduler;
import dev.sebi.sebchievements.services.TimerService;
import dev.sebi.sebchievements.services.WindowService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class SebchievementsPlugin extends JavaPlugin implements Listener {
    private Services services;
    private Granter granter;
    private PlayerDataStore store;
    private List<AdvancementHandler> handlers = List.of();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new PlayerDataStore(AdvancementIds.NAMESPACE);
        granter = new Granter(this);
        granter.register();

        PdcLongStore longs = new PdcLongStore(store);
        AfkService afk = new AfkService(() -> getConfig().getLong("afk_seconds", 120) * 1000);
        RecentDamageService damage = new RecentDamageService(
                getConfig().getLong("recent_damage_retention_seconds", 60) * 1000);
        TickScheduler tick = new TickScheduler(this);
        services = new Services(tick, new TimerService(longs), new StreakService(longs), new WindowService(),
                damage, new ProximityService(), afk, new CounterService(longs));

        // AFK sampling must run before any handler check
        tick.register("afk", afk::sample);
        tick.registerGlobal(() -> damage.purge(System.currentTimeMillis()));
        getServer().getPluginManager().registerEvents(afk, this);
        getServer().getPluginManager().registerEvents(new RecentDamageListener(damage), this);
        getServer().getPluginManager().registerEvents(this, this);

        handlers = HandlerValidator.validate(Handlers.ALL, AdvancementIds.PLUGIN, AdvancementIds.ALL,
                AdvancementIds.TRACKED, id -> Bukkit.getAdvancement(AdvancementIds.key(id)) != null, getLogger());
        for (AdvancementHandler h : handlers) {
            h.register(new HandlerContext(this, h.id(), granter, store, services, configFor(h.id())));
        }
        getLogger().info("Registered " + handlers.size() + " of " + AdvancementIds.PLUGIN.size() + " plugin handlers");

        RewardService rewards = new RewardService(this, store);
        RewardGuard rewardGuard = new RewardGuard(rewards);
        tick.register("reward_cap", rewardGuard::sweep);
        getServer().getPluginManager().registerEvents(rewardGuard, this);
        getServer().getPluginManager().registerEvents(new RewardMenuListener(), this);

        tick.start();
        AdvCommand command = new AdvCommand(this, granter, store, services, handlers, rewards);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                event -> event.registrar().register(command.build()));
    }

    @Override
    public void onDisable() {
        if (services != null) services.tick().stop();
    }

    /** Config section {@code advancements.<id>}; empty (never null) when the id has no entry. */
    private ConfigurationSection configFor(String id) {
        ConfigurationSection s = getConfig().getConfigurationSection("advancements." + id);
        return s != null ? s : new org.bukkit.configuration.MemoryConfiguration();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        granter.grantRoots(event.getPlayer());
    }
}

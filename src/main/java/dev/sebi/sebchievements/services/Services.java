package dev.sebi.sebchievements.services;

import java.util.UUID;

/** All shared services, built once at startup. */
public record Services(TickScheduler tick, TimerService timers, StreakService streaks, WindowService windows,
                       RecentDamageService damage, ProximityService proximity, AfkService afk,
                       CounterService counters, PlacedBlocks placed) {

    /** Clears all tracked state for one advancement and player ({@code /adv reset}). */
    public void resetState(UUID player, String id) {
        timers.reset(player, id);
        streaks.reset(player, id);
        counters.reset(player, id);
        windows.clear(player, id);
    }
}

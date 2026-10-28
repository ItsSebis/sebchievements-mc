package dev.sebi.sebchievements.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Horizontal distance covered in one continuous ride. Distance is added between two consecutive samples
 * only when both were on a counting surface; stepping into another vehicle, or a jump above the step cap
 * (portal, teleport), starts over. {@link #dismount} resets.
 */
public final class RideTracker {
    private static final class Ride {
        UUID vehicle;
        double x, z;
        boolean counted;
        double meters;
    }

    private final Map<UUID, Ride> rides = new HashMap<>();

    /** @return the distance of the current ride after this sample */
    public double update(UUID player, UUID vehicle, double x, double z, boolean counts, double maxStep) {
        Ride r = rides.get(player);
        if (r == null || !r.vehicle.equals(vehicle) || Math.hypot(x - r.x, z - r.z) > maxStep) {
            r = new Ride();
            r.vehicle = vehicle;
            rides.put(player, r);
        } else if (counts && r.counted) {
            r.meters += Math.hypot(x - r.x, z - r.z);
        }
        r.x = x;
        r.z = z;
        r.counted = counts;
        return r.meters;
    }

    public double distance(UUID player) {
        Ride r = rides.get(player);
        return r == null ? 0 : r.meters;
    }

    public void dismount(UUID player) {
        rides.remove(player);
    }
}

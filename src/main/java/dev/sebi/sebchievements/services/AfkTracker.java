package dev.sebi.sebchievements.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pure AFK logic. A player is active when they moved at least {@link #MOVE_BLOCKS} or turned at least
 * {@link #TURN_DEGREES} away from where they were when last counted as active, so nudges and
 * tiny jitters don't reset the clock.
 */
public final class AfkTracker {
    static final double MOVE_BLOCKS = 0.5;
    static final float TURN_DEGREES = 5f;

    private static final class Ref {
        double x, y, z;
        float yaw, pitch;
        long activeAt;
    }

    private final Map<UUID, Ref> refs = new HashMap<>();

    public void update(UUID player, double x, double y, double z, float yaw, float pitch, long now) {
        Ref r = refs.get(player);
        if (r == null) {
            r = new Ref();
            refs.put(player, r);
        } else if (!moved(r, x, y, z, yaw, pitch)) {
            return;
        }
        r.x = x;
        r.y = y;
        r.z = z;
        r.yaw = yaw;
        r.pitch = pitch;
        r.activeAt = now;
    }

    public boolean isAfk(UUID player, long now, long afkMillis) {
        Ref r = refs.get(player);
        return r != null && now - r.activeAt >= afkMillis;
    }

    public void remove(UUID player) {
        refs.remove(player);
    }

    private static boolean moved(Ref r, double x, double y, double z, float yaw, float pitch) {
        double dx = x - r.x, dy = y - r.y, dz = z - r.z;
        if (dx * dx + dy * dy + dz * dz >= MOVE_BLOCKS * MOVE_BLOCKS) return true;
        float dyaw = Math.abs(((yaw - r.yaw) % 360f + 540f) % 360f - 180f);
        return dyaw >= TURN_DEGREES || Math.abs(pitch - r.pitch) >= TURN_DEGREES;
    }
}

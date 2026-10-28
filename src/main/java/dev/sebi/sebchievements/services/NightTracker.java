package dev.sebi.sebchievements.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pure night-by-night bookkeeping for Pacifist. A player is "in" a night from its first in-window sample; the
 * night is clean only if they were present for the whole night (first seen within {@code slack} ticks of the start)
 * and every sample was clean. The outcome is reported at the first sample outside the window.
 */
public final class NightTracker {
    public enum Result { NONE, COMPLETED, FAILED }

    public record Outcome(Result result, long day) {
        static final Outcome NONE = new Outcome(Result.NONE, -1);
    }

    private static final class State {
        final long day;
        boolean ok;

        State(long day, boolean ok) {
            this.day = day;
            this.ok = ok;
        }
    }

    private final Map<UUID, State> states = new HashMap<>();

    /** @param day the night's day number; @param clean online, awake-and-moving, survival, in the Overworld */
    public Outcome sample(UUID player, long day, long timeOfDay, boolean clean, long start, long end, long slack) {
        boolean night = timeOfDay >= start && timeOfDay < end;
        State s = states.get(player);
        if (!night) {
            states.remove(player);
            return s == null ? Outcome.NONE : finish(s);
        }
        Outcome previous = Outcome.NONE;
        if (s != null && s.day != day) { // a whole day passed between samples
            previous = finish(s);
            s = null;
        }
        if (s == null) {
            s = new State(day, timeOfDay - start <= slack);
            states.put(player, s);
        }
        if (!clean) s.ok = false;
        return previous;
    }

    /** Marks the player's current night as failed (a mob kill, a skipped night). */
    public void disqualify(UUID player) {
        State s = states.get(player);
        if (s != null) s.ok = false;
    }

    /** Logging out mid-night fails it. */
    public Outcome leave(UUID player) {
        State s = states.remove(player);
        return s == null ? Outcome.NONE : new Outcome(Result.FAILED, s.day);
    }

    private static Outcome finish(State s) {
        return new Outcome(s.ok ? Result.COMPLETED : Result.FAILED, s.day);
    }
}

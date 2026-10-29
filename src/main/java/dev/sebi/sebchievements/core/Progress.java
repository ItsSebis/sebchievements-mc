package dev.sebi.sebchievements.core;

/** Current/target of a timed or streak advancement for one player. */
public record Progress(long current, long target, Unit unit) {
    public enum Unit { TIME_MILLIS, COUNT }

    public static Progress time(long currentMillis, long targetMillis) {
        return new Progress(currentMillis, targetMillis, Unit.TIME_MILLIS);
    }

    public static Progress count(long current, long target) {
        return new Progress(current, target, Unit.COUNT);
    }

    /** E.g. {@code 12m 40s / 20m} or {@code 7 / 10}. */
    public String format() {
        long shown = Math.min(current, target);
        return switch (unit) {
            case TIME_MILLIS -> duration(shown) + " / " + duration(target);
            case COUNT -> shown + " / " + target;
        };
    }

    public static String duration(long millis) {
        long total = Math.max(0, millis) / 1000;
        long h = total / 3600, m = total % 3600 / 60, s = total % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0 || sb.isEmpty()) sb.append(s).append("s ");
        return sb.toString().trim();
    }
}

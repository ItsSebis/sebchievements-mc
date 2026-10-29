package dev.sebi.sebchievements.build;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Parsed and validated form of {@code advancements.yml}. */
public record Catalog(Map<String, Tab> tabs, List<Entry> advancements) {

    public static final Set<String> FRAMES = Set.of("task", "goal", "challenge");
    public static final Set<String> MODES = Set.of("vanilla", "plugin");
    public static final Set<String> KINDS = Set.of("timed", "streak", "counter");

    public record Tab(String key, String title, String icon, String background) {}

    /**
     * @param criteria   vanilla: criterion name to {trigger, conditions}; plugin: unused
     * @param requirements vanilla only, optional: list of OR-groups of criterion names
     * @param pluginCriteria plugin only: criterion names a handler may award (default {@code done})
     * @param kind       null, "timed", "streak" or "counter"
     * @param difficulty 1-10 grade (see docs/difficulty.md), null if ungraded
     * @param reward     items handed out when earned (a loot table is generated), null for none
     * @param perk       a permanent, toggleable potion effect granted by the plugin (effect, amplifier, name, icon), null for none
     */
    public record Entry(String id, String tab, String parent, String title, String description, String icon,
                        String frame, boolean hidden, String mode, Map<String, Object> criteria,
                        List<List<String>> requirements, List<String> pluginCriteria, String kind,
                        String antiCheese, Integer difficulty, List<Map<String, Object>> reward,
                        Map<String, Object> perk) {
        public boolean isPlugin() {
            return "plugin".equals(mode);
        }

        public boolean isTracked() {
            return kind != null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Catalog parse(Object yaml) {
        Map<String, Object> root = asMap(yaml, "catalog");
        Map<String, Tab> tabs = new LinkedHashMap<>();
        for (var e : asMap(root.get("tabs"), "tabs").entrySet()) {
            Map<String, Object> t = asMap(e.getValue(), "tab " + e.getKey());
            tabs.put(e.getKey(), new Tab(e.getKey(), str(t, "title"), str(t, "icon"), str(t, "background")));
        }
        List<Entry> entries = new ArrayList<>();
        Object list = root.get("advancements");
        if (list instanceof List<?> l) {
            for (Object o : l) {
                Map<String, Object> m = asMap(o, "advancement entry");
                Map<String, Object> criteria = m.get("criteria") instanceof Map<?, ?> c ? (Map<String, Object>) c : null;
                List<String> pluginCriteria = m.get("criteria") instanceof List<?> c
                        ? c.stream().map(String::valueOf).toList() : List.of("done");
                List<List<String>> requirements = null;
                if (m.get("requirements") instanceof List<?> r) {
                    requirements = r.stream()
                            .map(g -> ((List<?>) g).stream().map(String::valueOf).toList()).toList();
                }
                entries.add(new Entry(str(m, "id"), str(m, "tab"),
                        m.get("parent") == null ? "root" : str(m, "parent"),
                        str(m, "title"), str(m, "description"), str(m, "icon"),
                        m.get("frame") == null ? "task" : str(m, "frame"),
                        Boolean.TRUE.equals(m.get("hidden")), str(m, "mode"),
                        criteria, requirements, pluginCriteria,
                        m.get("kind") == null ? null : str(m, "kind"),
                        m.get("anti_cheese") == null ? null : str(m, "anti_cheese"),
                        m.get("difficulty") instanceof Number n ? Integer.valueOf(n.intValue())
                                : m.get("difficulty") == null ? null : Integer.valueOf(-1),
                        rewardItems(m),
                        m.get("perk") instanceof Map<?, ?> pk ? (Map<String, Object>) pk : null));
            }
        }
        return new Catalog(tabs, entries);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rewardItems(Map<String, Object> entry) {
        Object raw = entry.get("reward");
        if (raw == null) return null;
        if (!(raw instanceof List<?> list)) {
            throw new IllegalArgumentException("advancement '" + entry.get("id") + "': reward must be a list of items");
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : list) out.add(asMap(o, "reward item of '" + entry.get("id") + "'"));
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o, String what) {
        if (o instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new IllegalArgumentException(what + " must be a mapping");
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }
}

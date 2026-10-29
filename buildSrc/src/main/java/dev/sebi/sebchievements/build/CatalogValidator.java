package dev.sebi.sebchievements.build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Collects every problem in the catalog so one build run reports them all. */
public final class CatalogValidator {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern ITEM = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private CatalogValidator() {}

    public static List<String> validate(Catalog catalog) {
        List<String> errors = new ArrayList<>();
        for (Catalog.Tab t : catalog.tabs().values()) {
            if (!ID.matcher(t.key()).matches()) errors.add("tab '" + t.key() + "': key must match " + ID.pattern());
            for (String[] f : new String[][] {{"title", t.title()}, {"icon", t.icon()}, {"background", t.background()}}) {
                if (f[1] == null || f[1].isBlank()) errors.add("tab '" + t.key() + "': missing " + f[0]);
            }
            if (t.icon() != null && !ITEM.matcher(t.icon()).matches()) errors.add("tab '" + t.key() + "': icon must be a namespaced id");
        }

        Map<String, Catalog.Entry> byId = new HashMap<>();
        Set<String> duplicates = new HashSet<>();
        for (Catalog.Entry e : catalog.advancements()) {
            if (e.id() == null || !ID.matcher(e.id()).matches()) {
                errors.add("advancement '" + e.id() + "': id must match " + ID.pattern());
                continue;
            }
            if (e.id().equals("root")) errors.add("advancement id 'root' is reserved");
            if (byId.putIfAbsent(e.id(), e) != null && duplicates.add(e.id())) errors.add("duplicate id '" + e.id() + "'");
        }

        for (Catalog.Entry e : catalog.advancements()) {
            String at = "advancement '" + e.id() + "': ";
            if (e.tab() == null || !catalog.tabs().containsKey(e.tab())) errors.add(at + "unknown tab '" + e.tab() + "'");
            if (!e.parent().equals("root") && !byId.containsKey(e.parent())) errors.add(at + "unknown parent '" + e.parent() + "'");
            if (hasParentCycle(e, byId)) errors.add(at + "parent chain loops");
            if (!Catalog.FRAMES.contains(e.frame())) errors.add(at + "invalid frame '" + e.frame() + "' (task | goal | challenge)");
            if (!Catalog.MODES.contains(e.mode())) errors.add(at + "invalid mode '" + e.mode() + "' (vanilla | plugin)");
            for (String[] f : new String[][] {{"title", e.title()}, {"description", e.description()}, {"icon", e.icon()}}) {
                if (f[1] == null || f[1].isBlank()) errors.add(at + "missing " + f[0]);
            }
            if (e.icon() != null && !ITEM.matcher(e.icon()).matches()) errors.add(at + "icon must be a namespaced id");
            if ("vanilla".equals(e.mode()) && (e.criteria() == null || e.criteria().isEmpty())) {
                errors.add(at + "mode vanilla needs criteria");
            }
            if (e.isPlugin() && e.criteria() != null) errors.add(at + "plugin criteria must be a list of names");
            if (e.isPlugin() && e.pluginCriteria().isEmpty()) errors.add(at + "plugin criteria list is empty");
            if (e.kind() != null && !Catalog.KINDS.contains(e.kind())) errors.add(at + "invalid kind '" + e.kind() + "' (timed | streak | counter)");
            if (e.isTracked() && !e.isPlugin()) errors.add(at + "timed/streak entries must be mode plugin");
            if (e.isTracked() && (e.antiCheese() == null || e.antiCheese().isBlank())) {
                errors.add(at + "timed/streak entries need an anti_cheese note");
            }
            if (e.difficulty() != null && (e.difficulty() < 1 || e.difficulty() > 10)) {
                errors.add(at + "difficulty must be a whole number from 1 to 10");
            }
            if (e.reward() != null) validateReward(e, at, errors);
            if (e.perk() != null) validatePerk(e, at, errors);
            if (e.requirements() != null && e.criteria() != null) {
                for (List<String> group : e.requirements()) {
                    for (String c : group) {
                        if (!e.criteria().containsKey(c)) errors.add(at + "requirements mention unknown criterion '" + c + "'");
                    }
                }
            }
        }
        return errors;
    }

    private static final Set<String> PERK_KEYS = Set.of("effect", "amplifier", "name", "icon");

    private static void validatePerk(Catalog.Entry e, String at, List<String> errors) {
        String where = at + "perk: ";
        if (!e.isPlugin()) errors.add(where + "needs mode plugin (the plugin applies the effect)");
        for (String key : e.perk().keySet()) {
            if (!PERK_KEYS.contains(key)) errors.add(where + "unknown key '" + key + "'");
        }
        if (!(e.perk().get("effect") instanceof String fx) || !ITEM.matcher(fx).matches()) {
            errors.add(where + "effect must be a namespaced effect id");
        }
        if (e.perk().containsKey("amplifier") && !(e.perk().get("amplifier") instanceof Integer a && a >= 0 && a <= 9)) {
            errors.add(where + "amplifier must be a whole number from 0 (level I) to 9");
        }
        if (!(e.perk().get("name") instanceof String n) || n.isBlank()) errors.add(where + "needs a name");
        if (!(e.perk().get("icon") instanceof String ic) || !ITEM.matcher(ic).matches()) {
            errors.add(where + "icon must be a namespaced item id");
        }
    }

    private static final Set<String> REWARD_KEYS = Set.of("item", "count", "name", "lore", "enchantments", "potion", "dye");
    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}");

    private static void validateReward(Catalog.Entry e, String at, List<String> errors) {
        if (e.reward().isEmpty()) errors.add(at + "reward list is empty");
        int n = 0;
        for (Map<String, Object> item : e.reward()) {
            String where = at + "reward item #" + (++n) + ": ";
            for (String key : item.keySet()) {
                if (!REWARD_KEYS.contains(key)) errors.add(where + "unknown key '" + key + "'");
            }
            if (!(item.get("item") instanceof String id) || !ITEM.matcher(id).matches()) {
                errors.add(where + "item must be a namespaced item id");
            }
            if (item.containsKey("count") && !(item.get("count") instanceof Integer c && c >= 1 && c <= 99)) {
                errors.add(where + "count must be a whole number from 1 to 99");
            }
            if (item.containsKey("name") && (!(item.get("name") instanceof String s) || s.isBlank())) {
                errors.add(where + "name must be a non-empty string");
            }
            if (item.containsKey("lore") && !(item.get("lore") instanceof List<?> l && l.stream().allMatch(x -> x instanceof String))) {
                errors.add(where + "lore must be a list of strings");
            }
            if (item.containsKey("potion") && (!(item.get("potion") instanceof String p) || !ITEM.matcher(p).matches())) {
                errors.add(where + "potion must be a namespaced potion id");
            }
            if (item.containsKey("dye") && !(item.get("dye") instanceof Integer
                    || item.get("dye") instanceof String d && HEX.matcher(d).matches())) {
                errors.add(where + "dye must be a whole number or \"#RRGGBB\"");
            }
            if (item.containsKey("enchantments")) {
                if (!(item.get("enchantments") instanceof Map<?, ?> m) || m.isEmpty()) {
                    errors.add(where + "enchantments must be a non-empty mapping of enchantment id to level");
                } else {
                    m.forEach((k, v) -> {
                        if (!(k instanceof String ks) || !ITEM.matcher(ks).matches()) errors.add(where + "bad enchantment id '" + k + "'");
                        if (!(v instanceof Integer lvl && lvl >= 1 && lvl <= 255)) errors.add(where + "enchantment level of '" + k + "' must be 1-255");
                    });
                }
            }
        }
    }

    private static boolean hasParentCycle(Catalog.Entry start, Map<String, Catalog.Entry> byId) {
        Set<String> seen = new HashSet<>();
        Catalog.Entry cur = start;
        while (cur != null && !cur.parent().equals("root")) {
            if (!seen.add(cur.id())) return true;
            cur = byId.get(cur.parent());
        }
        return false;
    }
}

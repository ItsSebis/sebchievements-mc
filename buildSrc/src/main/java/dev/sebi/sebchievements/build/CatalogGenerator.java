package dev.sebi.sebchievements.build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Reads the catalog, validates it and writes the datapack plus the {@code AdvancementIds} class. */
public final class CatalogGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    static final String PACK_DIR = "custom_datapack";
    static final String ID_PACKAGE = "dev.sebi.sebchievements";
    /** PDC key (namespace:key) that marks an item as the reward of an advancement; must match RewardItems.TAG. */
    static final String REWARD_TAG_KEY = "smp:reward_id";

    private CatalogGenerator() {}

    public static Catalog load(Path catalogFile) throws IOException {
        try (Reader r = Files.newBufferedReader(catalogFile, StandardCharsets.UTF_8)) {
            return Catalog.parse(new Yaml().load(r));
        }
    }

    public static void generate(Path catalogFile, String namespace, int dataMajor, int dataMinor,
                                Path resourcesDir, Path javaDir) throws IOException {
        Catalog catalog = load(catalogFile);
        List<String> errors = CatalogValidator.validate(catalog);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid advancement catalog " + catalogFile + ":\n  - "
                    + String.join("\n  - ", errors));
        }
        writeDatapack(catalog, namespace, dataMajor, dataMinor, resourcesDir.resolve(PACK_DIR));
        Path out = javaDir.resolve(ID_PACKAGE.replace('.', '/')).resolve("AdvancementIds.java");
        Files.createDirectories(out.getParent());
        Files.writeString(out, idsClass(catalog, namespace), StandardCharsets.UTF_8);
        Files.writeString(out.resolveSibling("RewardSpecs.java"), rewardSpecsClass(catalog), StandardCharsets.UTF_8);
        Files.writeString(out.resolveSibling("PerkSpecs.java"), perkSpecsClass(catalog), StandardCharsets.UTF_8);
    }

    static void writeDatapack(Catalog catalog, String ns, int major, int minor, Path pack) throws IOException {
        Files.createDirectories(pack);
        Map<String, Object> mcmeta = Map.of("pack", orderedMap(
                "description", "Sebchievements advancements (generated)",
                "min_format", major, "max_format", major));
        write(pack.resolve("pack.mcmeta"), mcmeta);

        Path adv = pack.resolve("data").resolve(ns).resolve("advancement");
        for (Catalog.Tab tab : catalog.tabs().values()) {
            Map<String, Object> display = orderedMap("icon", Map.of("id", tab.icon()), "title", tab.title(),
                    "description", tab.title(), "background", tab.background(),
                    "show_toast", false, "announce_to_chat", false);
            write(adv.resolve(tab.key()).resolve("root.json"), orderedMap(
                    "display", display,
                    "criteria", Map.of("joined", Map.of("trigger", "minecraft:impossible"))));
        }

        Map<String, Catalog.Entry> byId = catalog.advancements().stream()
                .collect(Collectors.toMap(Catalog.Entry::id, e -> e));
        for (Catalog.Entry e : catalog.advancements()) {
            Map<String, Object> json = new LinkedHashMap<>();
            String parentPath = e.parent().equals("root") ? e.tab() + "/root"
                    : byId.get(e.parent()).tab() + "/" + e.parent();
            json.put("parent", ns + ":" + parentPath);
            json.put("display", orderedMap("icon", Map.of("id", e.icon()), "title", e.title(),
                    "description", e.description(), "frame", e.frame(),
                    "show_toast", true, "announce_to_chat", true, "hidden", e.hidden()));
            if (e.isPlugin()) {
                Map<String, Object> criteria = new LinkedHashMap<>();
                for (String c : e.pluginCriteria()) criteria.put(c, Map.of("trigger", "minecraft:impossible"));
                json.put("criteria", criteria);
            } else {
                json.put("criteria", e.criteria());
                if (e.requirements() != null) json.put("requirements", e.requirements());
            }
            if (e.reward() != null) {
                json.put("rewards", Map.of("loot", List.of(ns + ":rewards/" + e.id())));
                write(pack.resolve("data").resolve(ns).resolve("loot_table").resolve("rewards").resolve(e.id() + ".json"),
                        rewardTable(e));
            }
            write(adv.resolve(e.tab()).resolve(e.id() + ".json"), json);
        }
    }

    /** One pool per reward item; every pool rolls once, so the player receives all of them. */
    static Map<String, Object> rewardTable(Catalog.Entry e) {
        List<Object> pools = new ArrayList<>();
        for (Map<String, Object> item : e.reward()) {
            List<Object> functions = new ArrayList<>();
            int count = item.get("count") instanceof Integer c ? c : 1;

            Map<String, Object> components = new LinkedHashMap<>();
            if (item.get("name") instanceof String name) components.put("minecraft:custom_name", plain(name, null));
            // every reward says where it came from, so a stray Tax Refund can be traced
            List<Object> lore = new ArrayList<>();
            if (item.get("lore") instanceof List<?> l) l.forEach(x -> lore.add(plain((String) x, "gray")));
            lore.add(plain("Sebchievement: " + e.title(), "dark_gray"));
            components.put("minecraft:lore", lore);
            // Bukkit's PersistentDataContainer on an item lives here; the plugin finds reward copies by this tag
            components.put("minecraft:custom_data", orderedMap("PublicBukkitValues",
                    orderedMap(REWARD_TAG_KEY, e.id())));
            if (item.get("dye") != null) components.put("minecraft:dyed_color", dye(item.get("dye")));
            functions.add(orderedMap("type", "minecraft:set_components", "components", components));

            if (item.get("enchantments") instanceof Map<?, ?> enchants) {
                functions.add(orderedMap("type", "minecraft:set_enchantments", "enchantments", new LinkedHashMap<>(enchants)));
            }
            if (item.get("potion") instanceof String potion) {
                functions.add(orderedMap("type", "minecraft:set_potion", "id", potion));
            }
            // Last on purpose: applying components to a stack larger than the item's maximum (2 helmets, 2 potions)
            // fails and silently drops them. The loot table splits oversized stacks on its own afterwards.
            if (count > 1) functions.add(orderedMap("type", "minecraft:set_count", "count", count));
            pools.add(orderedMap("rolls", 1, "entries", List.of(orderedMap(
                    "type", "minecraft:item", "name", item.get("item"), "modifier", functions))));
        }
        return orderedMap("type", "minecraft:advancement_reward", "pools", pools);
    }

    /** A text component that is not italic (custom names and lore are italic by default). */
    private static Map<String, Object> plain(String text, String color) {
        Map<String, Object> m = orderedMap("text", text, "italic", false);
        if (color != null) m.put("color", color);
        return m;
    }

    private static int dye(Object value) {
        return value instanceof Integer i ? i : Integer.parseInt(((String) value).substring(1), 16);
    }

    static String idsClass(Catalog catalog, String ns) {
        StringBuilder s = new StringBuilder();
        s.append("// GENERATED from src/main/catalog/advancements.yml - do not edit.\n");
        s.append("package ").append(ID_PACKAGE).append(";\n\n");
        s.append("import org.bukkit.NamespacedKey;\n\n");
        s.append("import java.util.List;\nimport java.util.Map;\nimport java.util.Set;\n\n");
        s.append("public final class AdvancementIds {\n");
        s.append("    public static final String NAMESPACE = \"").append(ns).append("\";\n\n");
        for (Catalog.Entry e : catalog.advancements()) {
            s.append("    public static final String ").append(e.id().toUpperCase()).append(" = \"").append(e.id()).append("\";\n");
        }
        s.append("\n    /** Tab keys; each has a root advancement {@code <ns>:<tab>/root}. */\n");
        s.append("    public static final List<String> TABS = ").append(listOf(catalog.tabs().keySet())).append(";\n\n");
        s.append("    /** Every advancement id to its path below the namespace, e.g. {@code silly/tax_evasion}. */\n");
        s.append("    public static final Map<String, String> PATHS = Map.ofEntries(");
        s.append(join(catalog.advancements().stream()
                .map(e -> "\n        Map.entry(\"" + e.id() + "\", \"" + e.tab() + "/" + e.id() + "\")").toList()));
        s.append(");\n\n");
        s.append("    public static final Set<String> ALL = PATHS.keySet();\n");
        s.append("    /** Entries with {@code mode: plugin}: each needs exactly one handler. */\n");
        s.append("    public static final Set<String> PLUGIN = ").append(setOf(catalog.advancements().stream()
                .filter(Catalog.Entry::isPlugin).map(Catalog.Entry::id).toList())).append(";\n");
        s.append("    /** Timed or streak entries: must report progress and obey the anti-cheese policy. */\n");
        s.append("    public static final Set<String> TRACKED = ").append(setOf(catalog.advancements().stream()
                .filter(Catalog.Entry::isTracked).map(Catalog.Entry::id).toList())).append(";\n");
        s.append("    /** Difficulty grade 1-10 per graded advancement (docs/difficulty.md). */\n");
        s.append("    public static final Map<String, Integer> DIFFICULTY = Map.ofEntries(");
        s.append(join(catalog.advancements().stream().filter(e -> e.difficulty() != null)
                .map(e -> "\n        Map.entry(\"" + e.id() + "\", " + e.difficulty() + ")").toList()));
        s.append(");\n");
        s.append("    /** Criterion names a handler may award, per plugin advancement. */\n");
        s.append("    public static final Map<String, List<String>> CRITERIA = Map.ofEntries(");
        s.append(join(catalog.advancements().stream().filter(Catalog.Entry::isPlugin)
                .map(e -> "\n        Map.entry(\"" + e.id() + "\", " + listOf(e.pluginCriteria()) + ")").toList()));
        s.append(");\n\n");
        s.append("    private AdvancementIds() {}\n\n");
        s.append("    public static NamespacedKey key(String id) {\n");
        s.append("        String path = PATHS.get(id);\n");
        s.append("        if (path == null) throw new IllegalArgumentException(\"Unknown advancement id: \" + id);\n");
        s.append("        return new NamespacedKey(NAMESPACE, path);\n    }\n\n");
        s.append("    public static NamespacedKey rootKey(String tab) {\n");
        s.append("        return new NamespacedKey(NAMESPACE, tab + \"/root\");\n    }\n}\n");
        return s.toString();
    }

    /** The reward items of every advancement as Java data, so the reward menu can rebuild them at runtime. */
    static String rewardSpecsClass(Catalog catalog) {
        StringBuilder s = new StringBuilder();
        s.append("// GENERATED from src/main/catalog/advancements.yml - do not edit.\n");
        s.append("package ").append(ID_PACKAGE).append(";\n\n");
        s.append("import dev.sebi.sebchievements.rewards.RewardItem;\n\n");
        s.append("import java.util.List;\nimport java.util.Map;\n\n");
        s.append("public final class RewardSpecs {\n");
        s.append("    /** Advancement id to its reward items, in catalog order; advancements without a reward are absent. */\n");
        s.append("    public static final Map<String, List<RewardItem>> ITEMS = Map.ofEntries(");
        List<String> entries = new ArrayList<>();
        for (Catalog.Entry e : catalog.advancements()) {
            if (e.reward() == null) continue;
            List<String> items = new ArrayList<>();
            for (Map<String, Object> item : e.reward()) {
                int count = item.get("count") instanceof Integer c ? c : 1;
                List<String> lore = item.get("lore") instanceof List<?> l
                        ? l.stream().map(x -> javaString((String) x)).toList() : List.of();
                List<String> enchants = new ArrayList<>();
                if (item.get("enchantments") instanceof Map<?, ?> m) {
                    m.forEach((k, v) -> enchants.add("Map.entry(" + javaString((String) k) + ", " + v + ")"));
                }
                items.add("new RewardItem(" + javaString((String) item.get("item")) + ", " + count + ", "
                        + (item.get("name") instanceof String n ? javaString(n) : "null") + ", "
                        + "List.of(" + String.join(", ", lore) + "), "
                        + "Map.ofEntries(" + String.join(", ", enchants) + "), "
                        + (item.get("potion") instanceof String p ? javaString(p) : "null") + ", "
                        + (item.get("dye") != null ? Integer.toString(dye(item.get("dye"))) : "null") + ", "
                        + javaString(item.get("reclaim") instanceof String r ? r : "free") + ")");
            }
            entries.add("\n        Map.entry(" + javaString(e.id()) + ", List.of(" + String.join(", ", items) + "))");
        }
        s.append(join(entries)).append(");\n\n");
        s.append("    private RewardSpecs() {}\n}\n");
        return s.toString();
    }

    /** The effect perks (advancement id to effect, level, display name and icon), applied by the plugin at runtime. */
    static String perkSpecsClass(Catalog catalog) {
        StringBuilder s = new StringBuilder();
        s.append("// GENERATED from src/main/catalog/advancements.yml - do not edit.\n");
        s.append("package ").append(ID_PACKAGE).append(";\n\n");
        s.append("import dev.sebi.sebchievements.rewards.PerkSpec;\n\n");
        s.append("import java.util.Map;\n\n");
        s.append("public final class PerkSpecs {\n");
        s.append("    /** Advancement id to the permanent effect perk it unlocks; advancements without one are absent. */\n");
        s.append("    public static final Map<String, PerkSpec> PERKS = Map.ofEntries(");
        List<String> entries = new ArrayList<>();
        for (Catalog.Entry e : catalog.advancements()) {
            if (e.perk() == null) continue;
            int amp = e.perk().get("amplifier") instanceof Integer a ? a : 0;
            entries.add("\n        Map.entry(" + javaString(e.id()) + ", new PerkSpec(" + javaString((String) e.perk().get("effect"))
                    + ", " + amp + ", " + javaString((String) e.perk().get("name")) + ", "
                    + javaString((String) e.perk().get("icon")) + "))");
        }
        s.append(join(entries)).append(");\n\n");
        s.append("    private PerkSpecs() {}\n}\n");
        return s.toString();
    }

    private static String javaString(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static String listOf(Iterable<String> items) {
        List<String> quoted = new ArrayList<>();
        for (String i : items) quoted.add("\"" + i + "\"");
        return "List.of(" + String.join(", ", quoted) + ")";
    }

    private static String setOf(List<String> items) {
        return "Set.of(" + String.join(", ", items.stream().map(i -> "\"" + i + "\"").toList()) + ")";
    }

    private static String join(List<String> parts) {
        return String.join(",", parts);
    }

    private static Map<String, Object> orderedMap(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static void write(Path file, Object json) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(json) + "\n", StandardCharsets.UTF_8);
    }
}

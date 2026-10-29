package dev.sebi.sebchievements.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/** The server silently ignores unknown loot-table keys, so the key names the generator emits are pinned here. */
class RewardTableTest {
    @SuppressWarnings("unchecked")
    private static Map<String, Object> table(String rewardYaml) {
        Catalog c = Catalog.parse(new Yaml().load("""
                tabs:
                  silly: { title: "Silly", icon: "minecraft:carved_pumpkin", background: "minecraft:block/hay_block_side" }
                advancements:
                  - id: a
                    tab: silly
                    title: Title
                    description: d
                    icon: minecraft:emerald
                    mode: plugin
                    reward: %s
                """.formatted(rewardYaml)));
        assertEquals(List.of(), CatalogValidator.validate(c));
        return CatalogGenerator.rewardTable(c.advancements().get(0));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entry(Map<String, Object> table, int pool) {
        Map<String, Object> p = (Map<String, Object>) ((List<Object>) table.get("pools")).get(pool);
        return (Map<String, Object>) ((List<Object>) p.get("entries")).get(0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void itemEntryUsesTheModifierKeyForItsFunctions() {
        Map<String, Object> t = table("[{item: \"minecraft:iron_nugget\", count: 30, name: \"Silver\"}]");
        assertEquals("minecraft:advancement_reward", t.get("type"));
        Map<String, Object> e = entry(t, 0);
        assertEquals("minecraft:item", e.get("type"));
        assertTrue(e.containsKey("modifier"), "26.3 loot entries use 'modifier'; 'functions' is ignored silently");
        assertFalse(e.containsKey("functions"));
        List<Map<String, Object>> fns = (List<Map<String, Object>>) e.get("modifier");
        assertEquals("minecraft:set_components", fns.get(0).get("type"));
        Map<String, Object> last = fns.get(fns.size() - 1);
        assertEquals("minecraft:set_count", last.get("type"), "count must come last (components fail on oversized stacks)");
        assertEquals(30, last.get("count"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void enchantmentsAndPotionsUseTheirOwnFunctions() {
        Map<String, Object> t = table("[{item: \"minecraft:bow\", enchantments: {\"minecraft:power\": 7}}, "
                + "{item: \"minecraft:potion\", potion: \"minecraft:night_vision\"}]");
        List<Map<String, Object>> bow = (List<Map<String, Object>>) entry(t, 0).get("modifier");
        assertTrue(bow.stream().anyMatch(f -> "minecraft:set_enchantments".equals(f.get("type"))));
        List<Map<String, Object>> potion = (List<Map<String, Object>>) entry(t, 1).get("modifier");
        assertTrue(potion.stream().anyMatch(f -> "minecraft:set_potion".equals(f.get("type"))));
    }

    @SuppressWarnings("unchecked")
    @Test
    void everyItemGetsItsOwnPoolSoThePlayerReceivesAllOfThem() {
        Map<String, Object> t = table("[{item: \"minecraft:saddle\"}, {item: \"minecraft:carrot_on_a_stick\"}]");
        assertEquals(2, ((List<Object>) t.get("pools")).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void everyRewardIsTaggedWithItsAdvancementIdForTheCopyLimit() {
        Map<String, Object> t = table("[{item: \"minecraft:emerald\"}]");
        List<Map<String, Object>> fns = (List<Map<String, Object>>) entry(t, 0).get("modifier");
        Map<String, Object> components = (Map<String, Object>) fns.get(0).get("components");
        Map<String, Object> custom = (Map<String, Object>) components.get("minecraft:custom_data");
        Map<String, Object> pdc = (Map<String, Object>) custom.get("PublicBukkitValues");
        assertEquals("a", pdc.get("smp:reward_id"), "Paper keeps item PDC under custom_data.PublicBukkitValues");
    }

    @Test
    void rewardSpecsClassListsEveryItemWithEscapedText() {
        Catalog c = Catalog.parse(new Yaml().load("""
                tabs:
                  silly: { title: "Silly", icon: "minecraft:carved_pumpkin", background: "minecraft:block/hay_block_side" }
                advancements:
                  - id: a
                    tab: silly
                    title: Title
                    description: d
                    icon: minecraft:emerald
                    mode: plugin
                    reward: [{item: "minecraft:bow", count: 2, name: "Say \\"hi\\"", enchantments: {"minecraft:power": 7}, dye: "#B02E26"}]
                  - id: b
                    tab: silly
                    title: Other
                    description: d
                    icon: minecraft:emerald
                    mode: plugin
                """));
        String src = CatalogGenerator.rewardSpecsClass(c);
        assertTrue(src.contains("Map.entry(\"a\", List.of(new RewardItem(\"minecraft:bow\", 2, \"Say \\\"hi\\\"\""), src);
        assertTrue(src.contains("Map.entry(\"minecraft:power\", 7)"), src);
        assertTrue(src.contains(Integer.toString(0xB02E26)), src);
        assertFalse(src.contains("Map.entry(\"b\""), "advancements without a reward are absent");
        assertTrue(src.contains(Integer.toString(0xB02E26) + ", \"free\")"), "reclaim defaults to free: " + src);
    }

    @Test
    void reclaimModeIsGeneratedAndValidated() {
        Catalog c = Catalog.parse(new Yaml().load("""
                tabs:
                  silly: { title: "Silly", icon: "minecraft:carved_pumpkin", background: "minecraft:block/hay_block_side" }
                advancements:
                  - id: a
                    tab: silly
                    title: A
                    description: d
                    icon: minecraft:emerald
                    mode: plugin
                    reward: [{item: "minecraft:totem_of_undying", reclaim: never}, {item: "minecraft:shield", reclaim: cost}]
                """));
        assertEquals(List.of(), CatalogValidator.validate(c));
        String src = CatalogGenerator.rewardSpecsClass(c);
        assertTrue(src.contains("null, null, \"never\")"), src);
        assertTrue(src.contains("null, null, \"cost\")"), src);
        assertTrue(CatalogGenerator.rewardTable(c.advancements().get(0)).toString().contains("minecraft:totem_of_undying"));
        assertFalse(CatalogGenerator.rewardTable(c.advancements().get(0)).toString().contains("reclaim"), "not a loot table key");

        Catalog bad = Catalog.parse(new Yaml().load("""
                tabs:
                  silly: { title: "Silly", icon: "minecraft:carved_pumpkin", background: "minecraft:block/hay_block_side" }
                advancements:
                  - id: a
                    tab: silly
                    title: A
                    description: d
                    icon: minecraft:emerald
                    mode: plugin
                    reward: [{item: "minecraft:shield", reclaim: sometimes}]
                """));
        assertTrue(String.join("\n", CatalogValidator.validate(bad)).contains("reclaim must be free, cost or never"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void everyRewardCarriesALoreLineWithTheAdvancementTitle() {
        Map<String, Object> t = table("[{item: \"minecraft:emerald\"}]");
        List<Map<String, Object>> fns = (List<Map<String, Object>>) entry(t, 0).get("modifier");
        Map<String, Object> components = (Map<String, Object>) fns.get(0).get("components");
        assertTrue(components.get("minecraft:lore").toString().contains("Sebchievement: Title"));
    }
}

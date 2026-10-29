package dev.sebi.sebchievements.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class CatalogValidatorTest {
    private static final String TABS = """
            tabs:
              silly: { title: "Silly", icon: "minecraft:carved_pumpkin", background: "minecraft:block/hay_block_side" }
            """;

    private static List<String> check(String advancements) {
        return CatalogValidator.validate(Catalog.parse(new Yaml().load(TABS + advancements)));
    }

    private static String entry(String extra) {
        return """
                advancements:
                  - id: a
                    tab: silly
                    title: A
                    description: d
                    icon: minecraft:emerald
                """ + extra.indent(4);
    }

    @Test
    void validVanillaEntryPasses() {
        assertEquals(List.of(), check(entry("""
                mode: vanilla
                criteria:
                  k: { trigger: "minecraft:tick" }
                """)));
    }

    @Test
    void validPluginEntryPasses() {
        assertEquals(List.of(), check(entry("mode: plugin")));
    }

    @Test
    void vanillaWithoutCriteriaFails() {
        assertTrue(check(entry("mode: vanilla")).get(0).contains("needs criteria"));
    }

    @Test
    void timedWithoutAntiCheeseFails() {
        List<String> errors = check(entry("mode: plugin\nkind: timed"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("anti_cheese"));
    }

    @Test
    void timedWithAntiCheesePasses() {
        assertEquals(List.of(), check(entry("mode: plugin\nkind: timed\nanti_cheese: not afk")));
    }

    @Test
    void unknownTabParentAndFrameAreAllReported() {
        List<String> errors = check(entry("mode: plugin\ntab: nope\nparent: ghost\nframe: epic"));
        assertEquals(3, errors.size());
    }

    @Test
    void duplicateIdsFail() {
        String two = entry("mode: plugin") + """
                  - id: a
                    tab: silly
                    title: B
                    description: d
                    icon: minecraft:emerald
                    mode: plugin
                """;
        assertTrue(check(two).stream().anyMatch(e -> e.contains("duplicate id 'a'")));
    }

    @Test
    void parentCycleFails() {
        String yaml = """
                advancements:
                  - { id: a, tab: silly, parent: b, title: A, description: d, icon: "minecraft:emerald", mode: plugin }
                  - { id: b, tab: silly, parent: a, title: B, description: d, icon: "minecraft:emerald", mode: plugin }
                """;
        assertTrue(check(yaml).stream().anyMatch(e -> e.contains("loops")));
    }

    @Test
    void validRewardPasses() {
        assertEquals(List.of(), check(entry("""
                mode: plugin
                difficulty: 4
                reward:
                  - { item: "minecraft:bow", name: "Long Shot", enchantments: { "minecraft:power": 7 } }
                  - { item: "minecraft:potion", potion: "minecraft:night_vision", count: 2, dye: "#B02E26" }
                """)));
    }

    @Test
    void badRewardFieldsAreAllReported() {
        List<String> errors = check(entry("""
                mode: plugin
                reward:
                  - { item: "bow", count: 0, colour: red, enchantments: { "minecraft:power": 300 }, dye: "red" }
                """));
        assertEquals(5, errors.size(), errors.toString());
    }

    @Test
    void emptyRewardListFails() {
        assertTrue(check(entry("mode: plugin\nreward: []")).get(0).contains("empty"));
    }

    @Test
    void difficultyMustBeOneToTen() {
        assertEquals(List.of(), check(entry("mode: plugin\ndifficulty: 10")));
        assertTrue(check(entry("mode: plugin\ndifficulty: 11")).get(0).contains("1 to 10"));
        assertTrue(check(entry("mode: plugin\ndifficulty: 0")).get(0).contains("1 to 10"));
        assertTrue(check(entry("mode: plugin\ndifficulty: hard")).get(0).contains("1 to 10"));
    }
}

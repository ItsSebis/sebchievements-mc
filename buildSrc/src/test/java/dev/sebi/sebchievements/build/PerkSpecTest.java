package dev.sebi.sebchievements.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class PerkSpecTest {
    private static Catalog catalog(String extra) {
        return Catalog.parse(new Yaml().load("""
                tabs:
                  silly: { title: "Silly", icon: "minecraft:carved_pumpkin", background: "minecraft:block/hay_block_side" }
                advancements:
                  - id: a
                    tab: silly
                    title: A
                    description: d
                    icon: minecraft:emerald
                    mode: plugin
                """ + extra.indent(4) + """
                  - id: b
                    tab: silly
                    title: B
                    description: d
                    icon: minecraft:emerald
                    mode: plugin
                """));
    }

    @Test
    void validPerkPasses() {
        Catalog c = catalog("perk: {effect: \"minecraft:speed\", amplifier: 1, name: \"Fast\", icon: \"minecraft:sugar\"}");
        assertEquals(List.of(), CatalogValidator.validate(c));
    }

    @Test
    void perkNeedsEffectNameIconAndAKnownKeySet() {
        List<String> errors = CatalogValidator.validate(catalog("perk: {amplifier: 12, color: red}"));
        String all = String.join("\n", errors);
        assertTrue(all.contains("effect must be a namespaced effect id"), all);
        assertTrue(all.contains("amplifier must be a whole number from 0"), all);
        assertTrue(all.contains("needs a name"), all);
        assertTrue(all.contains("icon must be a namespaced item id"), all);
        assertTrue(all.contains("unknown key 'color'"), all);
    }

    @Test
    void generatedClassListsOnlyAdvancementsWithAPerk() {
        String src = CatalogGenerator.perkSpecsClass(catalog(
                "perk: {effect: \"minecraft:haste\", amplifier: 1, name: \"Rush\", icon: \"minecraft:sugar\"}"));
        assertTrue(src.contains("Map.entry(\"a\", new PerkSpec(\"minecraft:haste\", 1, \"Rush\", \"minecraft:sugar\"))"), src);
        assertFalse(src.contains("Map.entry(\"b\""), src);
    }
}

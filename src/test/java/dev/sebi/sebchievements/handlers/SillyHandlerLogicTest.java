package dev.sebi.sebchievements.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sebi.sebchievements.handlers.silly.FashionDisasterHandler;
import dev.sebi.sebchievements.handlers.silly.OneMansTrashHandler;
import dev.sebi.sebchievements.handlers.support.TimedHandler;
import dev.sebi.sebchievements.services.TimerService.State;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SillyHandlerLogicTest {
    @Test
    void armorFamilyIsThePrefixOfRealArmorPieces() {
        assertEquals(Optional.of("IRON"), FashionDisasterHandler.family("IRON_HELMET"));
        assertEquals(Optional.of("GOLDEN"), FashionDisasterHandler.family("GOLDEN_BOOTS"));
        assertEquals(Optional.of("TURTLE"), FashionDisasterHandler.family("TURTLE_HELMET"));
        assertEquals(Optional.of("LEATHER"), FashionDisasterHandler.family("LEATHER_LEGGINGS"));
    }

    @Test
    void nonArmorIsNotAFamily() {
        assertEquals(Optional.empty(), FashionDisasterHandler.family("ELYTRA"));
        assertEquals(Optional.empty(), FashionDisasterHandler.family("CARVED_PUMPKIN"));
        assertEquals(Optional.empty(), FashionDisasterHandler.family("IRON_SWORD"));
    }

    @Test
    void junkIsListedAndNotEnchanted() {
        Set<String> junk = Set.of("LILY_PAD", "FISHING_ROD");
        assertTrue(OneMansTrashHandler.isJunk("LILY_PAD", false, junk));
        assertTrue(OneMansTrashHandler.isJunk("FISHING_ROD", false, junk));
        assertFalse(OneMansTrashHandler.isJunk("FISHING_ROD", true, junk), "enchanted rod is treasure");
        assertFalse(OneMansTrashHandler.isJunk("COD", false, junk));
    }

    @Test
    void timedPolicyPausesExcludedPlayersAndOtherwiseHoldsOrBreaks() {
        assertEquals(State.PAUSED, TimedHandler.decide(true, true));
        assertEquals(State.PAUSED, TimedHandler.decide(true, false));
        assertEquals(State.HELD, TimedHandler.decide(false, true));
        assertEquals(State.BROKEN, TimedHandler.decide(false, false));
    }
}

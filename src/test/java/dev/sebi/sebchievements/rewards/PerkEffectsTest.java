package dev.sebi.sebchievements.rewards;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PerkEffectsTest {
    @Test
    void ourOwnShortAmbientEffectIsRecognisedForRemoval() {
        assertTrue(PerkEffects.isOurs(1, 40, true, false, 1));
        assertTrue(PerkEffects.isOurs(1, 3, true, false, 1), "a nearly expired one is still ours");
    }

    @Test
    void aPotionOfTheSameLevelIsNeverRemoved() {
        assertFalse(PerkEffects.isOurs(1, 3600, false, true, 1), "long, with particles");
        assertFalse(PerkEffects.isOurs(1, 3600, true, false, 1), "a beacon-like long ambient effect");
        assertFalse(PerkEffects.isOurs(1, 40, false, true, 1), "short, but with particles");
    }

    @Test
    void anotherLevelIsNotOurs() {
        assertFalse(PerkEffects.isOurs(2, 40, true, false, 1));
    }

    @Test
    void anExistingEffectCoversThePerkOnlyIfItIsStrongEnoughAndOutlastsAnApplication() {
        assertTrue(PerkEffects.alreadyCovered(1, 1200, 1));
        assertTrue(PerkEffects.alreadyCovered(2, 1200, 1));
        assertFalse(PerkEffects.alreadyCovered(0, 1200, 1), "weaker");
        assertFalse(PerkEffects.alreadyCovered(1, 40, 1), "our own previous application must be refreshed");
        assertFalse(PerkEffects.alreadyCovered(1, 20, 1));
    }
}

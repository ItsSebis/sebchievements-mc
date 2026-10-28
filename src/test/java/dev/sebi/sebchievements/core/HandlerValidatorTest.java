package dev.sebi.sebchievements.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class HandlerValidatorTest {
    private static final Logger LOG = Logger.getLogger("test");

    private static AdvancementHandler handler(String id) {
        return new AdvancementHandler() {
            public String id() { return id; }
            public void register(HandlerContext ctx) {}
        };
    }

    private static AdvancementHandler reporting(String id) {
        class H implements AdvancementHandler, ProgressReporting {
            public String id() { return id; }
            public void register(HandlerContext ctx) {}
            public Optional<Progress> progress(Player p) { return Optional.empty(); }
        }
        return new H();
    }

    private List<AdvancementHandler> run(List<AdvancementHandler> hs) {
        return HandlerValidator.validate(hs, Set.of("a", "t"), Set.of("a", "t", "v"), Set.of("t"), id -> true, LOG);
    }

    @Test
    void validHandlersPass() {
        assertEquals(2, run(List.of(handler("a"), reporting("t"))).size());
    }

    @Test
    void unknownIdIsDropped() {
        assertEquals(0, run(List.of(handler("zzz"))).size());
    }

    @Test
    void handlerForVanillaEntryIsDropped() {
        assertEquals(0, run(List.of(handler("v"))).size());
    }

    @Test
    void duplicateHandlersAreAllDropped() {
        assertEquals(0, run(List.of(handler("a"), handler("a"))).size());
    }

    @Test
    void trackedWithoutProgressReportingIsDropped() {
        assertEquals(0, run(List.of(handler("t"))).size());
    }

    @Test
    void notLoadedAdvancementIsDropped() {
        List<AdvancementHandler> ok = HandlerValidator.validate(List.of(handler("a")), Set.of("a"), Set.of("a"),
                Set.of(), id -> false, LOG);
        assertEquals(0, ok.size());
    }

    @Test
    void oneBadHandlerDoesNotAffectOthers() {
        assertEquals(1, run(List.of(handler("a"), handler("zzz"))).size());
    }
}

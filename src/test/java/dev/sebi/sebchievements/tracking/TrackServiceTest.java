package dev.sebi.sebchievements.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.sebi.sebchievements.core.Progress;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TrackServiceTest {
    @Test
    void fillIsTheShareOfTheTargetClampedToTheBar() {
        assertEquals(0.5f, TrackService.fill(Progress.count(25, 50)));
        assertEquals(1f, TrackService.fill(Progress.count(80, 50)));
        assertEquals(0f, TrackService.fill(Progress.count(-3, 50)));
        assertEquals(0.25f, TrackService.fill(Progress.time(5_000, 20_000)));
    }

    @Test
    void zeroTargetGivesAnEmptyBar() {
        assertEquals(0f, TrackService.fill(Progress.count(5, 0)));
    }

    @Test
    void storedIdsRoundTrip() {
        Set<String> ids = new LinkedHashSet<>(List.of("skull_collector", "untouchable"));
        assertEquals(ids, TrackService.decode(TrackService.encode(ids), Set.of("skull_collector", "untouchable", "x")));
    }

    @Test
    void decodeDropsBlanksDuplicatesAndUnknownIds() {
        assertEquals(Set.of(), TrackService.decode("", Set.of("a")));
        assertEquals(Set.of(), TrackService.decode(null, Set.of("a")));
        assertEquals(List.of("a", "b"), List.copyOf(TrackService.decode("a,,a,gone,b", Set.of("a", "b"))));
    }
}

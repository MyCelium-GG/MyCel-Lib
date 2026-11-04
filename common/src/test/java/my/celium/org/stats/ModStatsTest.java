package my.celium.org.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import my.celium.org.metadata.ModMetadata;

class ModStatsTest {
    private static final ModMetadata META = ModMetadata.builder("statstest", "Stats Test", "1.0.0").build();

    @Test
    void rejectsBadAndDuplicateIds() {
        ModStats.Builder builder = ModStats.builder(META, "token").number("pets", () -> 1);
        assertThrows(IllegalArgumentException.class, () -> builder.number("pets", () -> 2));
        assertThrows(IllegalArgumentException.class, () -> builder.number("usage", () -> 2));
        assertThrows(IllegalArgumentException.class, () -> builder.string("Bad-Id", () -> "x"));
    }

    @Test
    void countsNothingWhileStatsAreOff() {
        ModStats stats = ModStats.builder(META, "token").numberMap("species", Map::of).start();
        assertFalse(stats.isRunning());
        stats.count("command_find");
        assertNull(stats.counters());
        assertEquals(1, stats.metrics().size());
    }
}

package my.celium.org.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.celium.org.TestPlatform;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.metadata.ModMetadata;

class ConfigTest {
    enum Mode {
        FAST,
        SLOW
    }

    @TempDir
    Path tempDir;

    private static final AtomicInteger IDS = new AtomicInteger();

    private ModMetadata meta() {
        return ModMetadata.builder("cfgtest" + IDS.incrementAndGet(), "Cfg Test", "1.0.0").build();
    }

    @BeforeEach
    void stubPlatform() {
        PlatformHolder.setForTesting(new TestPlatform(tempDir));
    }

    @Test
    void defaultsAndKinds() {
        ModMetadata meta = meta();
        ConfigBuilder builder = new ConfigBuilder(meta);
        ConfigValue<Boolean> bool = builder.booleanValue("enabled", true, "d");
        ConfigValue<Integer> integer = builder.intValue("radius", 8, 1, 32, "d");
        ConfigValue<Long> longValue = builder.longValue("big", 100L, 0L, 1000L, "d");
        ConfigValue<Double> doubleValue = builder.doubleValue("factor", 0.5, 0.0, 1.0, "d");
        ConfigValue<String> string = builder.stringValue("name", "hi", "d");
        ConfigValue<Mode> mode = builder.enumValue("mode", Mode.class, Mode.FAST, "d");
        ConfigValue<List<String>> list = builder.stringListValue("names", List.of("a"), "d");
        builder.build();

        assertTrue(bool.get());
        assertEquals(8, integer.get());
        assertEquals(100L, longValue.get());
        assertEquals(0.5, doubleValue.get());
        assertEquals("hi", string.get());
        assertEquals(Mode.FAST, mode.get());
        assertEquals(List.of("a"), list.get());
        assertEquals(ValueKind.BOOLEAN, bool.kind());
        assertEquals(ValueKind.ENUM, mode.kind());
    }

    @Test
    void setGetListenerAndReset() {
        ModMetadata meta = meta();
        ConfigBuilder builder = new ConfigBuilder(meta);
        ConfigValue<Integer> radius = builder.intValue("radius", 8, 1, 32, "d");
        builder.build();

        List<Integer> seen = new ArrayList<>();
        radius.onChange(seen::add);
        radius.set(10);
        assertEquals(10, radius.get());
        assertEquals(List.of(10), seen);
        assertFalse(radius.isDefault());
        radius.set(10); // no change -> no event
        assertEquals(List.of(10), seen);
        radius.reset();
        assertEquals(8, radius.get());
        assertTrue(radius.isDefault());
    }

    @Test
    void invalidProgrammaticSetsThrow() {
        ModMetadata meta = meta();
        ConfigBuilder builder = new ConfigBuilder(meta);
        ConfigValue<Integer> integer = builder.intValue("radius", 8, 1, 32, "d");
        ConfigValue<String> string = builder.stringValue("name", "hi", 4, "d");
        builder.build();

        assertThrows(IllegalArgumentException.class, () -> integer.set(33));
        assertThrows(IllegalArgumentException.class, () -> integer.set(0));
        assertThrows(IllegalArgumentException.class, () -> string.set("toolong"));
        assertThrows(NullPointerException.class, () -> integer.set(null));
    }

    @Test
    void diskDataIsClampedNeverRejected() throws IOException {
        ModMetadata meta = meta();
        ConfigBuilder builder = new ConfigBuilder(meta);
        ConfigValue<Integer> radius = builder.intValue("radius", 8, 1, 32, "d");
        ConfigValue<Mode> mode = builder.enumValue("mode", Mode.class, Mode.FAST, "d");
        MycelConfig config = builder.build();

        Path file = config.file();
        Files.writeString(file, "{\"radius\": 500, \"mode\": \"NOPE\"}", StandardCharsets.UTF_8);
        config.reload();

        assertEquals(32, radius.get());
        assertEquals(Mode.FAST, mode.get());
    }

    @Test
    void corruptFileFallsBackToDefaultsAndBacksUp() throws IOException {
        ModMetadata meta = meta();
        ConfigBuilder builder = new ConfigBuilder(meta);
        ConfigValue<Boolean> enabled = builder.booleanValue("enabled", true, "d");
        MycelConfig config = builder.build();

        Files.writeString(config.file(), "this is not json{{{", StandardCharsets.UTF_8);
        config.reload();

        assertTrue(enabled.get());
        assertTrue(Files.exists(config.file().resolveSibling(config.file().getFileName() + ".bak")));
    }

    @Test
    void saveWritesReadableJson() throws IOException {
        ModMetadata meta = meta();
        ConfigBuilder builder = new ConfigBuilder(meta);
        builder.category("general").booleanValue("enabled", true, "d");
        MycelConfig config = builder.build();

        String text = Files.readString(config.file(), StandardCharsets.UTF_8);
        assertTrue(text.contains("\"enabled\""));
        assertTrue(text.contains("\"general\""));
    }

    @Test
    void migrationTransformsOldFiles() throws IOException {
        ModMetadata meta = meta();
        // Simulate a v1 file with a different layout.
        Path file = tempDir.resolve(meta.id() + ".json");
        Files.writeString(file, "{\"old_section\": {\"size\": 5}}", StandardCharsets.UTF_8);

        ConfigBuilder builder = new ConfigBuilder(meta);
        builder.schemaVersion(2);
        builder.migrate(1, root -> {
            if (root.has("old_section")) {
                root.add("new_section", root.remove("old_section"));
            }
        });
        builder.category("new_section");
        ConfigValue<Integer> size = builder.intValue("size", 1, 1, 64, "d");
        builder.build();

        assertEquals(5, size.get());
    }

    @Test
    void categoriesAndResetAll() {
        ModMetadata meta = meta();
        ConfigBuilder builder = new ConfigBuilder(meta);
        ConfigValue<Boolean> root = builder.booleanValue("rootFlag", false, "d");
        builder.category("tweaks");
        ConfigValue<Boolean> tweaked = builder.booleanValue("tweak", false, "d");
        MycelConfig config = builder.build();

        assertEquals(List.of("", "tweaks"), config.categories());
        assertEquals(List.of(tweaked), config.valuesIn("tweaks"));
        root.set(true);
        tweaked.set(true);
        assertFalse(config.isDefault());
        config.resetAll();
        assertTrue(config.isDefault());
    }

    @Test
    void builderRejectsBadDefinitions() {
        ModMetadata meta = meta();
        ConfigBuilder builder = new ConfigBuilder(meta);
        builder.booleanValue("dup", true, "d");
        assertThrows(IllegalArgumentException.class, () -> builder.booleanValue("dup", false, "d"));
        assertThrows(IllegalArgumentException.class, () -> builder.intValue("bad", 5, 10, 1, "d"));
        assertThrows(IllegalArgumentException.class, () -> builder.intValue("bad", 99, 1, 10, "d"));
        assertThrows(IllegalArgumentException.class, () -> builder.category("has space"));
        assertThrows(IllegalStateException.class, () -> {
            builder.build();
            builder.build();
        });
    }

    @Test
    void stringListsAreDefensivelyCopied() {
        ModMetadata meta = meta();
        ConfigBuilder builder = new ConfigBuilder(meta);
        List<String> def = new ArrayList<>(List.of("a"));
        ConfigValue<List<String>> list = builder.stringListValue("names", def, "d");
        builder.build();

        def.add("mutated");
        assertEquals(List.of("a"), list.get());
        List<String> fetched = list.get();
        assertThrows(UnsupportedOperationException.class, () -> fetched.add("x"));
    }
}

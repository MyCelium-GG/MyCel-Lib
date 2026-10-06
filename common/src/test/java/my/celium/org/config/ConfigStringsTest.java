package my.celium.org.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.celium.org.TestPlatform;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.metadata.ModMetadata;

class ConfigStringsTest {
    enum Mode {
        FAST,
        SLOW
    }

    @TempDir
    Path tempDir;

    private static final AtomicInteger IDS = new AtomicInteger();

    private ModMetadata meta() {
        return ModMetadata.builder("strtest" + IDS.incrementAndGet(), "Str Test", "1.0.0").build();
    }

    @BeforeEach
    void stubPlatform() {
        PlatformHolder.setForTesting(new TestPlatform(tempDir));
    }

    @Test
    void pathLookupAndParsing() {
        ModMetadata meta = meta();
        ConfigBuilder builder = new ConfigBuilder(meta);
        ConfigValue<Boolean> enabled = builder.booleanValue("enabled", true, "d");
        builder.category("tweaks");
        ConfigValue<Integer> radius = builder.intValue("radius", 8, 1, 32, "d");
        ConfigValue<Mode> mode = builder.enumValue("mode", Mode.class, Mode.FAST, "d");
        ConfigValue<List<String>> names = builder.stringListValue("names", List.of("a"), "d");
        MycelConfig config = builder.build();

        assertEquals(enabled, ConfigStrings.find(config, "enabled"));
        assertEquals(radius, ConfigStrings.find(config, "tweaks.radius"));
        assertEquals(radius, config.find("tweaks.radius"));
        assertEquals("tweaks.radius", ConfigStrings.pathOf(radius));

        ConfigStrings.parseAndSet(enabled, "off");
        assertEquals(Boolean.FALSE, enabled.get());
        ConfigStrings.parseAndSet(radius, "12");
        assertEquals(12, radius.get());
        ConfigStrings.parseAndSet(mode, "slow");
        assertEquals(Mode.SLOW, mode.get());
        ConfigStrings.parseAndSet(names, "a, b ,c");
        assertEquals(List.of("a", "b", "c"), names.get());

        assertThrows(IllegalArgumentException.class, () -> ConfigStrings.parseAndSet(radius, "99"));
        assertThrows(IllegalArgumentException.class, () -> ConfigStrings.parseAndSet(mode, "nope"));
        assertNull(ConfigStrings.find(config, "nope"));
    }

    @Test
    void ambiguousBareKeyReturnsNull() {
        ModMetadata meta = meta();
        ConfigBuilder builder = new ConfigBuilder(meta);
        builder.booleanValue("flag", true, "d");
        builder.category("other");
        builder.booleanValue("flag", false, "d");
        MycelConfig config = builder.build();

        assertNull(ConfigStrings.find(config, "flag"));
        assertTrue(ConfigStrings.find(config, "other.flag") != null);
    }
}

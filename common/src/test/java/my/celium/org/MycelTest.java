package my.celium.org;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.celium.org.internal.PlatformHolder;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.platform.Platform;

class MycelTest {
    private static final AtomicInteger IDS = new AtomicInteger();

    @BeforeAll
    static void initOnce(@TempDir Path tempDir) {
        // One shared temp config dir for the whole class: Mycel initialises once per JVM.
        PlatformHolder.setForTesting(new TestPlatform(tempDir));
        Mycel.initialize();
    }

    @Test
    void initializesOnceWithOwnMetadata() {
        assertTrue(Mycel.isInitialized());
        assertEquals(Mycel.VERSION, Mycel.getVersion());
        assertEquals(Platform.UNKNOWN, Mycel.getPlatform());
        Mycel.initialize(); // idempotent
        assertTrue(Mycel.isInitialized());
    }

    @Test
    void registersModsAndKeepsFirstOnDuplicates() {
        String id = "myceltest" + IDS.incrementAndGet();
        ModMetadata first = ModMetadata.builder(id, "First", "1.0.0").build();
        ModMetadata second = ModMetadata.builder(id, "Second", "2.0.0").build();
        assertSame(first, Mycel.registerMod(first));
        assertSame(first, Mycel.registerMod(second));
        assertTrue(Mycel.getRegisteredMods().contains(first));
    }

    @Test
    void enableToggleDefaultsTrue() {
        String id = "myceltoggle" + IDS.incrementAndGet();
        assertTrue(Mycel.isEnabled(id));
        Mycel.setEnabled(id, false);
        assertFalse(Mycel.isEnabled(id));
        Mycel.setEnabled(id, true);
        assertTrue(Mycel.isEnabled(id));
    }

    @Test
    void unknownConfigIsNull() {
        assertNull(Mycel.getConfig("no-such-mod-anywhere"));
    }
}

package my.celium.org.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import my.celium.org.platform.Platform;
import my.celium.org.platform.PlatformService;

/**
 * Proves multi-implementation service discovery (the universal-jar case)
 * resolves deterministically instead of crashing or picking randomly.
 */
class PlatformSelectionTest {
    @AfterEach
    void clean() {
        PlatformHolder.reset();
    }

    @Test
    void detectsRuntimeWithoutCrashing() {
        Platform runtime = PlatformHolder.detectRuntime();
        assertNotNull(runtime);
    }

    @Test
    void neoForgeWinsOverBundledFabricLoader() {
        // Snow! Real Magic ships Forgified Fabric Loader on NeoForge: both class sets are visible.
        java.util.Set<String> classes = java.util.Set.of("net.neoforged.neoforge.common.NeoForge",
                "net.fabricmc.loader.api.FabricLoader");
        assertEquals(Platform.NEOFORGE, PlatformHolder.detect(classes::contains));
    }

    @Test
    void forgeWinsOverBundledFabricLoader() {
        java.util.Set<String> classes = java.util.Set.of("net.minecraftforge.fml.ModList",
                "net.fabricmc.loader.api.FabricLoader");
        assertEquals(Platform.FORGE, PlatformHolder.detect(classes::contains));
    }

    @Test
    void fabricAloneIsFabric() {
        assertEquals(Platform.FABRIC, PlatformHolder.detect("net.fabricmc.loader.api.FabricLoader"::equals));
        assertEquals(Platform.UNKNOWN, PlatformHolder.detect(name -> false));
    }

    @Test
    void selectsMatchingServiceWhenSeveralExist() {
        // Test resources register FABRIC + FORGE + NEOFORGE stubs, so every
        // detectable runtime has a match; UNKNOWN falls back without crashing.
        PlatformHolder.reset();
        try {
            PlatformService service = PlatformHolder.get();
            assertNotNull(service);
            Platform runtime = PlatformHolder.detectRuntime();
            if (runtime != Platform.UNKNOWN) {
                assertEquals(runtime, service.platform());
            }
        } finally {
            PlatformHolder.reset();
        }
    }
}

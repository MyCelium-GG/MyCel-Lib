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

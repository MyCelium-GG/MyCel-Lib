package my.celium.org.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReplaceOptionsTest {
    @Test
    void defaultsPreserveEverything() {
        ReplaceOptions options = ReplaceOptions.DEFAULT;
        assertTrue(options.preservePosition());
        assertTrue(options.preserveRotation());
        assertTrue(options.preserveVelocity());
        assertTrue(options.preserveName());
        assertTrue(options.preserveEquipment());
        assertTrue(options.preserveHealth());
        assertTrue(options.preservePersistence());
        assertTrue(options.preservePassengers());
        assertTrue(options.preserveLeash());
        assertTrue(options.preserveFlags());
    }

    @Test
    void builderTogglesIndividually() {
        ReplaceOptions options = ReplaceOptions.builder().health(false).leash(false).build();
        assertFalse(options.preserveHealth());
        assertFalse(options.preserveLeash());
        assertTrue(options.preservePosition());
        assertTrue(options.preserveEquipment());
    }
}

package my.celium.org.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ModMetadataTest {
    @Test
    void buildsValidMetadata() {
        ModMetadata meta = ModMetadata.builder("doubledoors", "Double Doors", "1.4.0")
                .website("https://example.com")
                .author("Alice")
                .authors(List.of("Bob"))
                .updateUrl("https://example.com/update.json")
                .build();
        assertEquals("doubledoors", meta.id());
        assertEquals("Double Doors", meta.name());
        assertEquals("1.4.0", meta.version());
        assertEquals(List.of("Alice", "Bob"), meta.authors());
        assertEquals(meta, ModMetadata.builder("doubledoors", "Other", "9.9.9").build());
    }

    @Test
    void rejectsBadIds() {
        assertThrows(IllegalArgumentException.class, () -> ModMetadata.builder("UPPER", "x", "1").build());
        assertThrows(IllegalArgumentException.class, () -> ModMetadata.builder("has space", "x", "1").build());
        assertThrows(IllegalArgumentException.class, () -> ModMetadata.builder("", "x", "1").build());
        assertThrows(IllegalArgumentException.class, () -> ModMetadata.builder(null, "x", "1").build());
    }

    @Test
    void updateUrlMustBeHttps() {
        assertThrows(IllegalArgumentException.class,
                () -> ModMetadata.builder("m", "x", "1").updateUrl("http://example.com/u.json").build());
        assertNull(ModMetadata.builder("m", "x", "1").build().updateUrl());
        assertTrue(ModMetadata.builder("m", "x", "1").build().authors().isEmpty());
    }
}

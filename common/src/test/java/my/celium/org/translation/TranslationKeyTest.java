package my.celium.org.translation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TranslationKeyTest {
    @Test
    void keysAreNamespaced() {
        assertEquals("mymod.config.enabled", MycelTranslations.key("mymod", "config.enabled"));
    }

    @Test
    void rejectsBlankParts() {
        assertThrows(IllegalArgumentException.class, () -> MycelTranslations.key("", "a"));
        assertThrows(IllegalArgumentException.class, () -> MycelTranslations.key("m", ""));
        assertThrows(IllegalArgumentException.class, () -> MycelTranslations.key(null, "a"));
    }

    @Test
    void componentCreationNeverThrows() {
        assertDoesNotThrow(() -> MycelTranslations.component("mymod", "some.key"));
    }

    @Test
    void missingKeyFallsBackToLiteral() {
        // Headless: no language is loaded, so this must take the fallback path, not crash.
        var component = MycelTranslations.componentOrFallback(
                "mycel-nonexistent-mod", "definitely.missing.key", "fallback text");
        assertEquals("fallback text", component.getString());
    }
}

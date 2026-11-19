package my.celium.org.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ModLangTest {
    @Test
    void fillsPlaceholdersInOrder() {
        assertEquals("3 x Awkward Potion", ModLang.format("%s x %s", 3, "Awkward Potion"));
    }

    @Test
    void fillsNumberedPlaceholders() {
        assertEquals("b then a", ModLang.format("%2$s then %1$s", "a", "b"));
    }

    @Test
    void keepsPercentSignsAndMissingArguments() {
        assertEquals("100% and %s", ModLang.format("100%% and %s"));
        assertEquals("50 %", ModLang.format("%s %", 50));
    }

    @Test
    void textWithoutPlaceholdersIsUntouched() {
        assertEquals("Hello", ModLang.format("Hello", "unused"));
    }

    @Test
    void readsTheModsOwnFilesAndFallsBackToEnglish() {
        ModLang lang = ModLang.of("mycel", ModLangTest.class);
        assertEquals("Reset all", lang.in("en_us", "config.reset_all"));
        assertEquals("Alles zurücksetzen", lang.in("de_de", "config.reset_all"));
        // A regional variant reads its family; an unknown language reads English.
        assertEquals("Alles zurücksetzen", lang.in("de_at", "config.reset_all"));
        assertEquals("Reset all", lang.in("xx_yy", "config.reset_all"));
        assertEquals("Pomóż ulepszać mody MyCelium", lang.in("pl_pl", "stats.title"));
        assertEquals("Ajustes de Test", lang.in("es_mx", "config.title", "Test"));
    }

    @Test
    void aMissingKeyShowsTheKey() {
        assertEquals("mycel.does.not.exist", ModLang.of("mycel", ModLangTest.class).in("en_us", "does.not.exist"));
    }
}

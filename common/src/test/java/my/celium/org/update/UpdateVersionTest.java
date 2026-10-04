package my.celium.org.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpdateVersionTest {
    @Test
    void numericSegmentsCompareNumerically() {
        assertTrue(UpdateChecker.compareVersions("1.2.0", "1.1.9") > 0);
        assertTrue(UpdateChecker.compareVersions("1.10.0", "1.9.9") > 0);
        assertEquals(0, UpdateChecker.compareVersions("1.2", "1.2.0"));
        assertTrue(UpdateChecker.compareVersions("1.2.1", "1.2") > 0);
        assertTrue(UpdateChecker.compareVersions("2.0.0", "10.0.0") < 0);
    }

    @Test
    void qualifiersLoseToReleases() {
        assertTrue(UpdateChecker.compareVersions("1.2.0", "1.2.0-beta") > 0);
        assertTrue(UpdateChecker.compareVersions("1.2.0-beta", "1.2.0-alpha") > 0);
        assertEquals(0, UpdateChecker.compareVersions("1.0.0", "1.0.0"));
    }

    @Test
    void tolerantOfOddInputs() {
        assertEquals(0, UpdateChecker.compareVersions("1", "1.0.0.0"));
        assertTrue(UpdateChecker.compareVersions("1.0.1", "1.0.0-anything") > 0);
    }
}

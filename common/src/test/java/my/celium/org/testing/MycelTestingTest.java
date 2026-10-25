package my.celium.org.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.celium.org.Mycel;
import my.celium.org.TestPlatform;
import my.celium.org.platform.Platform;

class MycelTestingTest {
    @AfterEach
    void restoreSharedPlatform(@TempDir Path dir) {
        // Other test classes in this JVM expect a platform to stay installed.
        MycelTesting.usePlatform(new TestPlatform(dir));
    }

    @Test
    void headlessPlatformUsesTheGivenConfigDir(@TempDir Path dir) {
        MycelTesting.useConfigDir(dir);
        assertEquals(dir, Mycel.getConfigDir());
        assertEquals(Platform.UNKNOWN, Mycel.getPlatform());
        assertFalse(Mycel.isClient());
        assertFalse(Mycel.isModLoaded("minecraft"));
    }
}

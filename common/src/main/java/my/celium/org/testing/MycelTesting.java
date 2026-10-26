package my.celium.org.testing;

import java.nio.file.Path;
import java.util.Objects;

import my.celium.org.internal.PlatformHolder;
import my.celium.org.platform.Platform;
import my.celium.org.platform.PlatformService;

/**
 * Support for headless unit tests of mods built on Mycel: no loader, no game.
 *
 * <pre>{@code
 * @BeforeAll
 * static void boot(@TempDir Path dir) {
 *     MycelTesting.useConfigDir(dir);   // config/*.json go to the temp dir
 *     MyMod.init();
 * }
 * }</pre>
 *
 * <p>Call it before the first Mycel call of the test JVM. Never call it from
 * mod code that runs in the game.
 */
public final class MycelTesting {
    private MycelTesting() {
    }

    /**
     * Runs Mycel on a headless platform ({@link Platform#UNKNOWN}, server
     * side, no other mods loaded) whose config folder is {@code configDir}.
     *
     * @param configDir where config files go, usually a JUnit {@code @TempDir}
     * @return the platform now in use
     */
    public static PlatformService useConfigDir(Path configDir) {
        PlatformService platform = new HeadlessPlatform(Objects.requireNonNull(configDir, "configDir"));
        usePlatform(platform);
        return platform;
    }

    /**
     * Runs Mycel on {@code platform}, for tests that need more control (e.g. {@code isModLoaded}).
     *
     * @param platform the platform to use
     */
    public static void usePlatform(PlatformService platform) {
        PlatformHolder.setForTesting(Objects.requireNonNull(platform, "platform"));
    }

    /** Forgets the test platform; the next Mycel call looks for a real one again. */
    public static void reset() {
        PlatformHolder.reset();
    }

    private record HeadlessPlatform(Path configDir) implements PlatformService {
        @Override
        public Platform platform() {
            return Platform.UNKNOWN;
        }

        @Override
        public String loaderVersion() {
            return "test";
        }

        @Override
        public String minecraftVersion() {
            return "test";
        }

        @Override
        public boolean isClient() {
            return false;
        }

        @Override
        public boolean isModLoaded(String modId) {
            return false;
        }

        @Override
        public String getModVersion(String modId) {
            return null;
        }
    }
}

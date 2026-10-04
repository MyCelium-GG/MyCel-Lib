package my.celium.org.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import my.celium.org.logging.MycelLog;
import my.celium.org.platform.Platform;
import my.celium.org.platform.PlatformService;

/**
 * Internal {@link ServiceLoader} holder for the active {@link PlatformService}.
 *
 * <p>Not public API: prefer the delegates on {@code Mycel} ({@code getPlatform()},
 * {@code isClient()}, ...). Each loader jar ships exactly one service file under
 * {@code META-INF/services}.
 */
public final class PlatformHolder {
    private static volatile PlatformService service;

    private PlatformHolder() {
    }

    /** Returns the active platform service, loading it on first use. */
    public static PlatformService get() {
        PlatformService current = service;
        if (current != null) {
            return current;
        }
        synchronized (PlatformHolder.class) {
            if (service == null) {
                service = load();
            }
            return service;
        }
    }

    /** Test-only override (e.g. headless unit tests). */
    public static synchronized void setForTesting(PlatformService override) {
        service = override;
    }

    /** Clears any cached/overridden service. */
    public static synchronized void reset() {
        service = null;
    }

    /**
     * Detects the current runtime by loader classes actually present. Used to
     * pick the right implementation when several are visible (universal jar).
     * Never initializes the probed classes.
     */
    public static Platform detectRuntime() {
        ClassLoader loader = PlatformHolder.class.getClassLoader();
        if (present(loader, "net.fabricmc.loader.api.FabricLoader")) {
            return Platform.FABRIC;
        }
        if (present(loader, "net.neoforged.neoforge.common.NeoForge")) {
            return Platform.NEOFORGE;
        }
        if (present(loader, "net.minecraftforge.fml.ModList")) {
            return Platform.FORGE;
        }
        return Platform.UNKNOWN;
    }

    private static boolean present(ClassLoader loader, String name) {
        try {
            Class.forName(name, false, loader);
            return true;
        } catch (LinkageError | ClassNotFoundException e) {
            return false;
        }
    }

    private static PlatformService load() {
        List<PlatformService> found = new ArrayList<>();
        for (PlatformService candidate : ServiceLoader.load(PlatformService.class)) {
            found.add(candidate);
        }
        if (found.isEmpty()) {
            MycelLog.warn("No PlatformService found; using UNKNOWN fallback (unit-test mode?)");
            return FallbackPlatformService.INSTANCE;
        }
        if (found.size() == 1) {
            return found.get(0);
        }
        Platform runtime = detectRuntime();
        for (PlatformService candidate : found) {
            if (candidate.platform() == runtime) {
                MycelLog.debug("Selected {} platform service for {}", candidate.getClass().getName(), runtime);
                return candidate;
            }
        }
        MycelLog.warn("No PlatformService matches runtime {}; using the first of {}",
                runtime, found.get(0).getClass().getName());
        return found.get(0);
    }

    /** Last-resort service so headless code paths fail gracefully instead of crashing. */
    private enum FallbackPlatformService implements PlatformService {
        INSTANCE;

        @Override
        public Platform platform() {
            return Platform.UNKNOWN;
        }

        @Override
        public String loaderVersion() {
            return "unknown";
        }

        @Override
        public String minecraftVersion() {
            return "unknown";
        }

        @Override
        public boolean isClient() {
            return false;
        }

        @Override
        public java.nio.file.Path configDir() {
            return java.nio.file.Path.of("config");
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

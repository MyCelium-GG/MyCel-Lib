package my.celium.org.internal.stub;

import java.nio.file.Path;

import my.celium.org.platform.Platform;
import my.celium.org.platform.PlatformService;

/** Test-only platform stub (listed in test services resources). */
public final class StubFabricPlatform implements PlatformService {
    @Override
    public Platform platform() {
        return Platform.FABRIC;
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
    public Path configDir() {
        return Path.of("config");
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

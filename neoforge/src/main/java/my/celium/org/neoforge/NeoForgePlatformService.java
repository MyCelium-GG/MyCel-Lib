package my.celium.org.neoforge;

import java.nio.file.Path;

import my.celium.org.platform.Platform;
import my.celium.org.platform.PlatformService;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

/**
 * NeoForge implementation of {@link PlatformService}.
 * Loaded via {@link java.util.ServiceLoader} from the NeoForge jar.
 */
public final class NeoForgePlatformService implements PlatformService {
    @Override
    public Platform platform() {
        return Platform.NEOFORGE;
    }

    @Override
    public String loaderVersion() {
        return modVersion("neoforge");
    }

    @Override
    public String minecraftVersion() {
        return modVersion("minecraft");
    }

    @Override
    public boolean isClient() {
        try {
            return FMLEnvironment.getDist().isClient();
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public Path configDir() {
        try {
            return FMLPaths.CONFIGDIR.get();
        } catch (RuntimeException e) {
            return Path.of("config");
        }
    }

    @Override
    public boolean isModLoaded(String modId) {
        try {
            return ModList.get() != null && ModList.get().isLoaded(modId);
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public String getModVersion(String modId) {
        try {
            if (ModList.get() == null) {
                return null;
            }
            return ModList.get().getModContainerById(modId)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String modVersion(String modId) {
        try {
            if (ModList.get() == null) {
                return "unknown";
            }
            return ModList.get().getModContainerById(modId)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse("unknown");
        } catch (RuntimeException e) {
            return "unknown";
        }
    }
}

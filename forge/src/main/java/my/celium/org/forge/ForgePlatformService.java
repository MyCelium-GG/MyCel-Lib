package my.celium.org.forge;

import java.nio.file.Path;

import my.celium.org.platform.Platform;
import my.celium.org.platform.PlatformService;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Forge implementation of {@link PlatformService}.
 * Loaded via {@link java.util.ServiceLoader} from the Forge jar.
 */
public final class ForgePlatformService implements PlatformService {
    @Override
    public Platform platform() {
        return Platform.FORGE;
    }

    @Override
    public String loaderVersion() {
        return modVersion("forge");
    }

    @Override
    public String minecraftVersion() {
        return modVersion("minecraft");
    }

    @Override
    public boolean isClient() {
        try {
            return FMLEnvironment.dist.isClient();
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
            return ModList.isLoaded(modId);
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public String getModVersion(String modId) {
        try {
            return ModList.getModContainerById(modId)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String modVersion(String modId) {
        try {
            return ModList.getModContainerById(modId)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse("unknown");
        } catch (RuntimeException e) {
            return "unknown";
        }
    }
}

package my.celium.org.fabric;

import java.nio.file.Path;

import my.celium.org.platform.Platform;
import my.celium.org.platform.PlatformService;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric implementation of {@link PlatformService}.
 * Loaded via {@link java.util.ServiceLoader} from the Fabric jar.
 */
public final class FabricPlatformService implements PlatformService {
    @Override
    public Platform platform() {
        return Platform.FABRIC;
    }

    @Override
    public String loaderVersion() {
        try {
            return FabricLoader.getInstance().getModContainer("fabricloader")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    @Override
    public String minecraftVersion() {
        try {
            return FabricLoader.getInstance().getModContainer("minecraft")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    @Override
    public boolean isClient() {
        try {
            return FabricLoader.getInstance().getEnvironmentType()
                    == net.fabricmc.api.EnvType.CLIENT;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public String getModVersion(String modId) {
        try {
            return FabricLoader.getInstance().getModContainer(modId)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }
}

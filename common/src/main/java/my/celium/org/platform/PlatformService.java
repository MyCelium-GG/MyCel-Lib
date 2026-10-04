package my.celium.org.platform;

import java.nio.file.Path;

/**
 * Loader-specific behaviour behind a tiny interface.
 *
 * <p>Common code must never import Fabric/Forge/NeoForge APIs directly.
 * Each loader module provides exactly one implementation, discovered via
 * {@link java.util.ServiceLoader}. Implementations live in the loader
 * modules (e.g. {@code my.celium.org.fabric.FabricPlatformService}).
 */
public interface PlatformService {
    /** Which loader this implementation is for. */
    Platform platform();

    /** Loader version string, e.g. Fabric Loader {@code "0.19.5"}. */
    String loaderVersion();

    /** Minecraft version string, e.g. {@code "26.3"}. */
    String minecraftVersion();

    /** {@code true} on the physical/logical client. */
    boolean isClient();

    /** Directory where per-mod config files live (the loader's {@code config} folder). */
    Path configDir();

    /** Whether another mod is present. Never throws. */
    boolean isModLoaded(String modId);

    /** Version of another mod, or {@code null} when absent/unknown. Never throws. */
    String getModVersion(String modId);
}

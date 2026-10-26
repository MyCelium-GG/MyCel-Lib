package my.celium.org.platform;

import java.nio.file.Path;
import java.util.List;

/**
 * Loader-specific behaviour behind a tiny interface.
 *
 * <p>Common code must never import Fabric/Forge/NeoForge APIs directly.
 * Each loader module provides exactly one implementation, discovered via
 * {@link java.util.ServiceLoader}. Implementations live in the loader
 * modules (e.g. {@code my.celium.org.fabric.FabricPlatformService}).
 */
public interface PlatformService {
    /**
     * Which loader this implementation is for.
     *
     * @return the loader
     */
    Platform platform();

    /**
     * Loader version string, e.g. Fabric Loader {@code "0.19.5"}.
     *
     * @return the loader version
     */
    String loaderVersion();

    /**
     * Minecraft version string, e.g. {@code "26.3"}.
     *
     * @return the Minecraft version
     */
    String minecraftVersion();

    /**
     * {@code true} on the physical/logical client.
     *
     * @return whether this is a client
     */
    boolean isClient();

    /**
     * Directory where per-mod config files live (the loader's {@code config} folder).
     *
     * @return the config directory
     */
    Path configDir();

    /**
     * Whether another mod is present. Never throws.
     *
     * @param modId the mod id
     * @return {@code true} when the loader has it
     */
    boolean isModLoaded(String modId);

    /**
     * Version of another mod, or {@code null} when absent/unknown. Never throws.
     *
     * @param modId the mod id
     * @return its version, or {@code null}
     */
    String getModVersion(String modId);

    /**
     * Every mod the loader has loaded. Empty when unknown. Never throws.
     *
     * @return the loaded mods
     */
    default List<LoadedMod> loadedMods() {
        return List.of();
    }

    /**
     * The game (client) or server directory that holds {@code config}, {@code logs} and {@code mods}.
     *
     * @return the game directory
     */
    default Path gameDir() {
        Path parent = configDir().toAbsolutePath().getParent();
        return parent == null ? Path.of(".").toAbsolutePath() : parent;
    }
}

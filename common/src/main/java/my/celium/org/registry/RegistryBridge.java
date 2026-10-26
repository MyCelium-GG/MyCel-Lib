package my.celium.org.registry;

import java.util.function.Supplier;

import my.celium.org.platform.Platform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Loader-specific registration backend.
 *
 * <p>Fabric registers eagerly against the vanilla registry; Forge/NeoForge
 * collect into a {@code DeferredRegister} flushed on their mod event bus.
 * Each loader module provides one implementation via {@link java.util.ServiceLoader}.
 */
public interface RegistryBridge {
    /**
     * Which loader this bridge is for (used to pick among several in one jar).
     *
     * @return the loader, {@link Platform#UNKNOWN} by default
     */
    default Platform platform() {
        return Platform.UNKNOWN;
    }

    /**
     * Registers {@code factory} under {@code modId:name} in {@code registry}.
     * The factory must not run before registration time.
     *
     * @param <T>      what the registry holds
     * @param registry the vanilla registry
     * @param key      the registry's key
     * @param modId    the namespace
     * @param name     the path
     * @param factory  creates the object
     * @return supplies the object once it is registered
     */
    <T> Supplier<T> register(Registry<T> registry, ResourceKey<Registry<T>> key, String modId, String name,
            Supplier<T> factory);

    /**
     * Hooks deferred registration into the loader (mod event bus on
     * Forge/NeoForge; no-op on Fabric). Called once by the loader entrypoint.
     *
     * @param modEventBus the mod event bus, or {@code null} on Fabric
     */
    void init(Object modEventBus);
}

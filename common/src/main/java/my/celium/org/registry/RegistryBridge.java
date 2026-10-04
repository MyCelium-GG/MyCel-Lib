package my.celium.org.registry;

import java.util.function.Supplier;

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
     * Registers {@code factory} under {@code modId:name} in {@code registry}.
     * The factory must not run before registration time.
     */
    <T> Supplier<T> register(Registry<T> registry, ResourceKey<Registry<T>> key, String modId, String name,
            Supplier<T> factory);

    /**
     * Hooks deferred registration into the loader (mod event bus on
     * Forge/NeoForge; no-op on Fabric). Called once by the loader entrypoint.
     */
    void init(Object modEventBus);
}

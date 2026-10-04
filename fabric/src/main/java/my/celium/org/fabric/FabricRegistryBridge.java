package my.celium.org.fabric;

import java.util.function.Supplier;

import my.celium.org.registry.RegistryBridge;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Fabric registry backend: registers eagerly against the vanilla registries.
 * Valid during mod entrypoint execution (before registries freeze).
 */
public final class FabricRegistryBridge implements RegistryBridge {
    @Override
    public <T> Supplier<T> register(Registry<T> registry, ResourceKey<Registry<T>> key, String modId, String name,
            Supplier<T> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(modId, name);
        T value = Registry.register(registry, id, factory.get());
        return () -> value;
    }

    @Override
    public void init(Object modEventBus) {
        // Fabric has no deferred registry event; nothing to hook up.
    }
}

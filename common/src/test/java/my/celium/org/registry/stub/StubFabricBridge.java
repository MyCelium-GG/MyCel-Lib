package my.celium.org.registry.stub;

import java.util.function.Supplier;

import my.celium.org.platform.Platform;
import my.celium.org.registry.RegistryBridge;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/** Test-only bridge that runs factories eagerly (like the Fabric backend). */
public final class StubFabricBridge implements RegistryBridge {
    @Override
    public Platform platform() {
        return Platform.FABRIC;
    }

    @Override
    public <T> Supplier<T> register(Registry<T> registry, ResourceKey<Registry<T>> key, String modId, String name,
            Supplier<T> factory) {
        T value = factory.get();
        return () -> value;
    }

    @Override
    public void init(Object modEventBus) {
    }
}

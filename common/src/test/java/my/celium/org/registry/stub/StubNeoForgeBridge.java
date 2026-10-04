package my.celium.org.registry.stub;

import java.util.function.Supplier;

import my.celium.org.platform.Platform;
import my.celium.org.registry.RegistryBridge;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Test-only bridge that never runs factories (like a deferred backend).
 * Listed first so fallback paths stay deterministic too.
 */
public final class StubNeoForgeBridge implements RegistryBridge {
    @Override
    public Platform platform() {
        return Platform.NEOFORGE;
    }

    @Override
    public <T> Supplier<T> register(Registry<T> registry, ResourceKey<Registry<T>> key, String modId, String name,
            Supplier<T> factory) {
        return () -> null;
    }

    @Override
    public void init(Object modEventBus) {
    }
}

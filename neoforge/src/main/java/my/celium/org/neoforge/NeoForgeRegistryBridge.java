package my.celium.org.neoforge;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import my.celium.org.platform.Platform;
import my.celium.org.registry.RegistryBridge;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge registry backend: collects into {@code DeferredRegister}s flushed
 * on the mod event bus.
 *
 * <p>Order-safe: registers created before {@link #init} are hooked then;
 * registers created later (e.g. by dependent mods constructed afterwards)
 * hook immediately.
 */
public final class NeoForgeRegistryBridge implements RegistryBridge {
    @Override
    public Platform platform() {
        return Platform.NEOFORGE;
    }

    private final Map<String, DeferredRegister<?>> registers = new LinkedHashMap<>();
    private final Set<DeferredRegister<?>> hooked = new HashSet<>();
    private volatile IEventBus bus;

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> Supplier<T> register(Registry<T> registry, ResourceKey<Registry<T>> key, String modId,
            String name, Supplier<T> factory) {
        String mapKey = modId + "@" + key.identifier();
        DeferredRegister<T> deferred = (DeferredRegister<T>) registers.computeIfAbsent(mapKey,
                k -> DeferredRegister.create(key, modId));
        hook(deferred);
        return deferred.register(name, factory)::get;
    }

    @Override
    public synchronized void init(Object modEventBus) {
        if (!(modEventBus instanceof IEventBus eventBus)) {
            throw new IllegalArgumentException("NeoForge registry bridge needs the mod IEventBus");
        }
        this.bus = eventBus;
        for (DeferredRegister<?> deferred : registers.values()) {
            hook(deferred);
        }
    }

    private void hook(DeferredRegister<?> deferred) {
        if (bus != null && hooked.add(deferred)) {
            deferred.register(bus);
        }
    }
}

package my.celium.org.forge;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import my.celium.org.registry.RegistryBridge;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;

/**
 * Forge registry backend: collects into {@code DeferredRegister}s flushed on
 * the mod bus group. Order-safe for dependent mods constructed later.
 */
public final class ForgeRegistryBridge implements RegistryBridge {
    private final Map<String, DeferredRegister<?>> registers = new LinkedHashMap<>();
    private final Set<DeferredRegister<?>> hooked = new HashSet<>();
    private volatile BusGroup busGroup;

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
        if (!(modEventBus instanceof BusGroup group)) {
            throw new IllegalArgumentException("Forge registry bridge needs the mod BusGroup");
        }
        this.busGroup = group;
        for (DeferredRegister<?> deferred : registers.values()) {
            hook(deferred);
        }
    }

    private void hook(DeferredRegister<?> deferred) {
        if (busGroup != null && hooked.add(deferred)) {
            deferred.register(busGroup);
        }
    }
}

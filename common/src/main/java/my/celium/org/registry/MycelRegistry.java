package my.celium.org.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;

import my.celium.org.logging.MycelLog;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.platform.Platform;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Minimal deferred registration covering the repetitive cases (items, blocks,
 * entity types) without a universal registry framework.
 *
 * <pre>{@code
 * MycelRegistry<Item> items = MycelRegistry.items(META);
 * DeferredEntry<Item> widget = items.registerWithKey("widget",
 *         key -> new Item(new Item.Properties().setId(key)));
 * }</pre>
 *
 * <p>Minecraft 26.x refuses items and blocks whose properties carry no id, so
 * use {@link #registerWithKey} (or {@link #key}) for those.
 *
 * <p>Names must be lowercase {@code [a-z0-9_/.-]} paths. Registration timing is
 * owned by the loader bridge: call {@code register} during mod construction /
 * entrypoint execution. Resolving ({@link DeferredEntry#get()}) before the
 * loader creates the object throws - read entries lazily (in handlers, not in
 * static initialisers).
 */
public final class MycelRegistry<T> {
    private final ModMetadata mod;
    private final Registry<T> registry;
    private final ResourceKey<Registry<T>> key;

    private MycelRegistry(ModMetadata mod, Registry<T> registry, ResourceKey<Registry<T>> key) {
        this.mod = mod;
        this.registry = registry;
        this.key = key;
    }

    public static MycelRegistry<Item> items(ModMetadata mod) {
        return new MycelRegistry<>(mod, BuiltInRegistries.ITEM, Registries.ITEM);
    }

    public static MycelRegistry<Block> blocks(ModMetadata mod) {
        return new MycelRegistry<>(mod, BuiltInRegistries.BLOCK, Registries.BLOCK);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static MycelRegistry<EntityType<?>> entityTypes(ModMetadata mod) {
        return new MycelRegistry(mod, BuiltInRegistries.ENTITY_TYPE, Registries.ENTITY_TYPE);
    }

    /**
     * Registers {@code factory} under {@code modid:name}. The factory runs at
     * the loader's registration time, never earlier.
     */
    public DeferredEntry<T> register(String name, Supplier<T> factory) {
        checkName(name);
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        return new DeferredEntry<>(RegistryBridges.register(registry, key, mod.id(), name, factory));
    }

    /**
     * Like {@link #register}, but hands the factory the entry's own key, which
     * {@code Item.Properties.setId} and {@code BlockBehaviour.Properties.setId} need.
     */
    public DeferredEntry<T> registerWithKey(String name, Function<ResourceKey<T>, T> factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        ResourceKey<T> entryKey = key(name);
        return register(name, () -> factory.apply(entryKey));
    }

    /** The key {@code modid:name} in this registry. */
    public ResourceKey<T> key(String name) {
        checkName(name);
        return ResourceKey.create(key, Identifier.fromNamespaceAndPath(mod.id(), name));
    }

    private static void checkName(String name) {
        if (name == null || !name.matches("[a-z0-9_/\\-.]+")) {
            throw new IllegalArgumentException("Invalid registry name: " + name);
        }
    }

    /**
     * Hooks deferred registration into the loader. Called once by each loader
     * entrypoint with its mod event bus (ignored on Fabric).
     *
     * @hidden
     */
    public static void initBridges(Object modEventBus) {
        RegistryBridges.init(modEventBus);
    }

    /** Internal bridge lookup. Not public API. */
    static final class RegistryBridges {
        private static volatile RegistryBridge bridge;

        private RegistryBridges() {
        }

        static synchronized <T> Supplier<T> register(Registry<T> registry, ResourceKey<Registry<T>> key,
                String modId, String name, Supplier<T> factory) {
            return bridge().register(registry, key, modId, name, factory);
        }

        public static synchronized void init(Object modEventBus) {
            bridge().init(modEventBus);
        }

        private static RegistryBridge bridge() {
            if (bridge == null) {
                List<RegistryBridge> found = new ArrayList<>();
                for (RegistryBridge candidate : ServiceLoader.load(RegistryBridge.class)) {
                    found.add(candidate);
                }
                if (found.isEmpty()) {
                    throw new IllegalStateException("No RegistryBridge on the classpath");
                }
                if (found.size() == 1) {
                    bridge = found.get(0);
                } else {
                    // Universal jar: pick the bridge matching the detected runtime.
                    Platform runtime = my.celium.org.internal.PlatformHolder.detectRuntime();
                    bridge = found.stream()
                            .filter(candidate -> candidate.platform() == runtime)
                            .findFirst()
                            .orElseGet(() -> {
                                MycelLog.warn("No RegistryBridge matches runtime {}; using the first",
                                        runtime);
                                return found.get(0);
                            });
                }
            }
            return bridge;
        }

        static synchronized void resetForTesting() {
            bridge = null;
        }
    }
}

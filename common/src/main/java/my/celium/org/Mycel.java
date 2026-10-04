package my.celium.org;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import my.celium.org.config.ConfigBuilder;
import my.celium.org.config.ConfigValue;
import my.celium.org.config.MycelConfig;
import my.celium.org.internal.ClientProxy;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.logging.MycelLog;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.platform.Platform;
import my.celium.org.platform.PlatformService;
import my.celium.org.update.UpdateChecker;
import net.minecraft.client.gui.screens.Screen;

/**
 * Mycel — the shared core library for the MyCelium ecosystem.
 *
 * <p>A dependent mod's entire integration looks like this:
 * <pre>{@code
 * public static final ModMetadata META =
 *     ModMetadata.builder("mymod", "My Mod", "1.0.0").build();
 *
 * public static final ConfigValue<Boolean> ENABLED;
 * public static final MycelConfig CONFIG;
 *
 * static {
 *     Mycel.initialize();          // once, from the loader entrypoint
 *     Mycel.registerMod(META);
 *     ConfigBuilder builder = Mycel.config(META);
 *     ENABLED = builder.booleanValue("enabled", true, "Whether the feature runs.");
 *     CONFIG = builder.build();
 * }
 * }</pre>
 *
 * <p>Initialization is idempotent and ordered: platform detection, global
 * config, then registration of dependent mods. Expensive or blocking work
 * (update checks) always happens asynchronously.
 */
public final class Mycel {
    /** Mycel's own mod id. */
    public static final String MOD_ID = "mycel";

    /** Mycel's version. Must match {@code gradle.properties}. */
    public static final String VERSION = "1.1.0";

    /** Mycel's own metadata (registered automatically on {@link #initialize()}). */
    public static final ModMetadata METADATA =
            ModMetadata.builder(MOD_ID, "Mycel", VERSION)
                    .website("https://github.com/MyCelium-GG/MyCel-Lib")
                    .author("MyCelium")
                    .build();

    private static volatile boolean initialized;
    private static final Map<String, ModMetadata> MODS = new LinkedHashMap<>();
    private static final Map<String, Boolean> ENABLED = new ConcurrentHashMap<>();
    private static final Map<String, MycelConfig> CONFIGS = new LinkedHashMap<>();
    private static volatile ConfigValue<Boolean> updateCheckerFlag;

    private Mycel() {
    }

    /**
     * Initialises Mycel exactly once: detects the platform, loads
     * {@code config/mycel.json} and registers Mycel itself. Loader entrypoints
     * call this first; extra calls are ignored with a debug line.
     */
    public static synchronized void initialize() {
        if (initialized) {
            MycelLog.debug("Mycel.initialize() called twice; ignoring");
            return;
        }
        initialized = true;
        PlatformService platform = PlatformHolder.get();
        registerMod(METADATA);
        ConfigBuilder global = new ConfigBuilder(METADATA);
        ConfigValue<Boolean> flag = global.booleanValue("enableUpdateChecker", true,
                "Whether Mycel may check for updates to registered mods (async, cached, never blocks).");
        global.build(); // ConfigBuilder.build() registers the handle with Mycel.
        updateCheckerFlag = flag;
        UpdateChecker.setGloballyEnabled(flag.get());
        flag.onChange(UpdateChecker::setGloballyEnabled);
        MycelLog.info("Mycel {} initialized on {} (loader {}, MC {})", VERSION,
                platform.platform().displayName(), platform.loaderVersion(), platform.minecraftVersion());
    }

    /** Whether {@link #initialize()} has run. */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Registers a dependent mod. Duplicate ids keep the first registration
     * (warns). Queues an async update check when the mod opted in.
     */
    public static synchronized ModMetadata registerMod(ModMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata");
        ModMetadata existing = MODS.get(metadata.id());
        if (existing != null) {
            MycelLog.warnMod(metadata.id(), "Mod already registered; keeping the first registration");
            return existing;
        }
        MODS.put(metadata.id(), metadata);
        MycelLog.infoMod(metadata.id(), "Registered {} {}", metadata.name(), metadata.version());
        if (initialized) {
            UpdateChecker.check(metadata);
        }
        return metadata;
    }

    /** All registered mods in registration order. */
    public static synchronized Collection<ModMetadata> getRegisteredMods() {
        return Collections.unmodifiableList(new ArrayList<>(MODS.values()));
    }

    /** Starts a configuration for {@code metadata} (10–30 lines for a typical mod). */
    public static ConfigBuilder config(ModMetadata metadata) {
        return new ConfigBuilder(Objects.requireNonNull(metadata, "metadata"));
    }

    /** The live config for a mod id, or {@code null} when the mod built none. */
    public static synchronized MycelConfig getConfig(String modId) {
        return CONFIGS.get(modId);
    }

    /** Called by {@link ConfigBuilder#build()}; one config per mod. */
    public static synchronized void registerConfig(MycelConfig config) {
        Objects.requireNonNull(config, "config");
        if (CONFIGS.containsKey(config.modId())) {
            throw new IllegalStateException("Config for '" + config.modId() + "' already registered");
        }
        CONFIGS.put(config.modId(), config);
    }

    /**
     * Whether a mod's functionality is enabled. Defaults to {@code true} so a
     * missing toggle never disables a feature by accident.
     */
    public static boolean isEnabled(String modId) {
        return ENABLED.getOrDefault(modId, Boolean.TRUE);
    }

    /** Enables/disables a mod at runtime without a restart. */
    public static void setEnabled(String modId, boolean enabled) {
        Objects.requireNonNull(modId, "modId");
        ENABLED.put(modId, enabled);
        MycelLog.infoMod(modId, enabled ? "Functionality enabled" : "Functionality disabled");
    }

    /** Mycel's version. */
    public static String getVersion() {
        return VERSION;
    }

    public static Platform getPlatform() {
        return PlatformHolder.get().platform();
    }

    /** Minecraft version string. */
    public static String getMinecraftVersion() {
        return PlatformHolder.get().minecraftVersion();
    }

    /** Loader version string. */
    public static String getLoaderVersion() {
        return PlatformHolder.get().loaderVersion();
    }

    /** Whether this is a client environment. */
    public static boolean isClient() {
        return PlatformHolder.get().isClient();
    }

    /** The loader's {@code config} directory. */
    public static Path getConfigDir() {
        return PlatformHolder.get().configDir();
    }

    public static boolean isModLoaded(String modId) {
        return PlatformHolder.get().isModLoaded(modId);
    }

    public static String getModVersion(String modId) {
        return PlatformHolder.get().getModVersion(modId);
    }

    /**
     * Creates the config screen for {@code modId} (client only). Returns
     * {@code null} on dedicated servers or when the mod has no config — so it
     * is always safe to call; check the result for {@code null}.
     */
    public static Screen openConfigScreen(Screen parent, String modId) {
        Object screen = ClientProxy.openConfigScreen(parent, modId);
        return screen instanceof Screen created ? created : null;
    }
}

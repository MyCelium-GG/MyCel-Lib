package my.celium.org;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import my.celium.org.config.ConfigBuilder;
import my.celium.org.config.ConfigValue;
import my.celium.org.config.MycelConfig;
import my.celium.org.diag.MycelDump;
import my.celium.org.internal.ClientProxy;
import my.celium.org.event.MycelEvents;
import my.celium.org.event.ServerLifecycleEvent;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.internal.UpdateNotices;
import my.celium.org.logging.MycelLog;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.platform.LoadedMod;
import my.celium.org.platform.Platform;
import my.celium.org.platform.PlatformService;
import my.celium.org.update.UpdateChecker;
import net.minecraft.client.gui.screens.Screen;

/**
 * Mycel - the shared core library for the MyCelium ecosystem.
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
    public static final String VERSION = "1.4.1";

    /** Mycel's own metadata (registered automatically on {@link #initialize()}). */
    public static final ModMetadata METADATA =
            ModMetadata.builder(MOD_ID, "Mycel", VERSION)
                    .website("https://github.com/MyCelium-GG/MyCel-Lib")
                    .author("MyCelium")
                    .updateUrl("https://wiki.mycelium-gg.org/updates/mycel.json")
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
        PlatformService platform = PlatformHolder.get();
        registerMod(METADATA);
        ConfigBuilder global = new ConfigBuilder(METADATA);
        ConfigValue<Boolean> flag = global.booleanValue("enableUpdateChecker", true,
                "Whether Mycel may check for updates to registered mods (async, cached, never blocks).");
        ConfigValue<Boolean> dumpUploads = global.booleanValue("allowDumpUploads", true,
                "Whether /mycel dump may upload its report to pastes.dev. Off: reports are only saved to mycel-dumps/.");
        ConfigValue<Boolean> notices = global.booleanValue("notifyOpsOfUpdates", true,
                "Whether operators (and the singleplayer host) get one chat line per session listing mod updates.");
        global.build(); // ConfigBuilder.build() registers the handle with Mycel.
        UpdateNotices.setEnabled(notices.get());
        notices.onChange(UpdateNotices::setEnabled);
        MycelEvents.onPlayerJoin(event -> UpdateNotices.onJoin(event.player()));
        MycelEvents.onServerLifecycle(event -> {
            if (event.phase() == ServerLifecycleEvent.Phase.STOPPED) {
                UpdateNotices.clear();
            }
        });
        MycelDump.setUploadsEnabled(dumpUploads.get());
        dumpUploads.onChange(MycelDump::setUploadsEnabled);
        updateCheckerFlag = flag;
        UpdateChecker.setGloballyEnabled(flag.get());
        flag.onChange(UpdateChecker::setGloballyEnabled);
        // Only now is the user's enableUpdateChecker choice known, so mods that
        // registered before initialize() get their check here instead of never.
        initialized = true;
        for (ModMetadata mod : MODS.values()) {
            UpdateChecker.check(mod);
        }
        MycelLog.info("Mycel {} initialized on {} (loader {}, MC {})", VERSION,
                platform.platform().displayName(), platform.loaderVersion(), platform.minecraftVersion());
    }

    /**
     * Whether {@link #initialize()} has run.
     *
     * @return {@code true} once it has run
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Registers a dependent mod. Duplicate ids keep the first registration
     * (warns). Queues an async update check when the mod opted in.
     *
     * @param metadata the mod to register
     * @return the registered metadata: {@code metadata}, or the first one for a duplicate id
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

    /**
     * All registered mods in registration order.
     *
     * @return an unmodifiable snapshot
     */
    public static synchronized Collection<ModMetadata> getRegisteredMods() {
        return Collections.unmodifiableList(new ArrayList<>(MODS.values()));
    }

    /**
     * Starts a configuration for {@code metadata} (10-30 lines for a typical mod).
     *
     * @param metadata the mod the config belongs to
     * @return a new builder; finish it with {@link ConfigBuilder#build()}
     */
    public static ConfigBuilder config(ModMetadata metadata) {
        return new ConfigBuilder(Objects.requireNonNull(metadata, "metadata"));
    }

    /**
     * The live config for a mod id, or {@code null} when the mod built none.
     *
     * @param modId the mod id
     * @return the config, or {@code null}
     */
    public static synchronized MycelConfig getConfig(String modId) {
        return CONFIGS.get(modId);
    }

    /**
     * Called by {@link ConfigBuilder#build()}; one config per mod.
     *
     * @param config the built config
     * @hidden
     */
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
     *
     * @param modId the mod id
     * @return whether it is enabled
     */
    public static boolean isEnabled(String modId) {
        return ENABLED.getOrDefault(modId, Boolean.TRUE);
    }

    /**
     * Enables/disables a mod at runtime without a restart.
     *
     * @param modId   the mod id
     * @param enabled the new state
     */
    public static void setEnabled(String modId, boolean enabled) {
        Objects.requireNonNull(modId, "modId");
        ENABLED.put(modId, enabled);
        MycelLog.infoMod(modId, enabled ? "Functionality enabled" : "Functionality disabled");
    }

    /**
     * Mycel's version.
     *
     * @return the same as {@link #VERSION}
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * The loader Mycel is running on.
     *
     * @return the loader, {@link Platform#UNKNOWN} in unit tests
     */
    public static Platform getPlatform() {
        return PlatformHolder.get().platform();
    }

    /**
     * Every mod the loader has loaded (not only Mycel mods). Empty when unknown.
     *
     * @return the loaded mods
     */
    public static List<LoadedMod> getLoadedMods() {
        return PlatformHolder.get().loadedMods();
    }

    /**
     * Minecraft version string.
     *
     * @return e.g. {@code "26.3"}
     */
    public static String getMinecraftVersion() {
        return PlatformHolder.get().minecraftVersion();
    }

    /**
     * Loader version string.
     *
     * @return e.g. {@code "0.19.5"} on Fabric
     */
    public static String getLoaderVersion() {
        return PlatformHolder.get().loaderVersion();
    }

    /**
     * Whether this is a client environment.
     *
     * @return {@code true} on the physical client
     */
    public static boolean isClient() {
        return PlatformHolder.get().isClient();
    }

    /**
     * The loader's {@code config} directory.
     *
     * @return the directory that holds every mod's config file
     */
    public static Path getConfigDir() {
        return PlatformHolder.get().configDir();
    }

    /**
     * Whether another mod is present. Never throws.
     *
     * @param modId the mod id
     * @return {@code true} when the loader has it
     */
    public static boolean isModLoaded(String modId) {
        return PlatformHolder.get().isModLoaded(modId);
    }

    /**
     * Version of another mod. Never throws.
     *
     * @param modId the mod id
     * @return its version, or {@code null} when absent or unknown
     */
    public static String getModVersion(String modId) {
        return PlatformHolder.get().getModVersion(modId);
    }

    /**
     * Creates the config screen for {@code modId} (client only). Returns
     * {@code null} on dedicated servers or when the mod has no config - so it
     * is always safe to call; check the result for {@code null}.
     *
     * @param parent the screen to return to when it closes
     * @param modId  the mod whose config to edit
     * @return the screen, or {@code null}
     */
    public static Screen openConfigScreen(Screen parent, String modId) {
        Object screen = ClientProxy.openConfigScreen(parent, modId);
        return screen instanceof Screen created ? created : null;
    }
}

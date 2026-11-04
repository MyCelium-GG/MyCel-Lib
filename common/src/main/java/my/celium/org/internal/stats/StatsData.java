package my.celium.org.internal.stats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import my.celium.org.Mycel;
import my.celium.org.config.ConfigValue;
import my.celium.org.config.MycelConfig;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.platform.LoadedMod;
import my.celium.org.update.UpdateChecker;
import my.celium.org.update.UpdateResult;

/**
 * The values behind Mycel's custom metrics. No FastStats types here, so it loads (and is
 * testable) without the SDK. Everything is computed when FastStats asks, about every 30 minutes.
 *
 * @hidden
 */
public final class StatsData {
    /**
     * Widely used mods we check for by id, to know what MyCelium mods run next to (compatibility).
     * Only these ids are ever reported; the rest of a mod list stays private.
     */
    static final Set<String> KNOWN_MODS = Set.of(
            "sodium", "iris", "lithium", "ferritecore", "modernfix", "entityculling", "immediatelyfast",
            "c2me", "krypton", "distanthorizons", "embeddium", "rubidium", "oculus", "indium",
            "jei", "rei", "emi", "jade", "wthit", "appleskin", "modmenu", "controlling", "mousetweaks",
            "inventoryprofilesnext", "journeymap", "xaerominimap", "xaeroworldmap", "voxelmap",
            "create", "farmersdelight", "supplementaries", "quark", "waystones", "travelersbackpack",
            "sophisticatedbackpacks", "curios", "accessories", "trinkets", "geckolib", "architectury",
            "cloth_config", "yet_another_config_lib_v3", "fabric_language_kotlin", "kotlinforforge",
            "voicechat", "essential", "worldedit", "luckperms", "ftbchunks", "ftbquests", "betterf3",
            "darkmode", "faststats");

    private static final Map<String, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();

    private StatsData() {
    }

    /**
     * Counts one use of something (a command, a dump upload) until the next successful send.
     *
     * @param what a lowercase name like {@code dump_upload}
     */
    public static void count(String what) {
        COUNTERS.computeIfAbsent(what, ignored -> new AtomicInteger()).incrementAndGet();
    }

    /** The counts since the last send, or {@code null} when nothing happened. */
    static Map<String, Integer> counters() {
        Map<String, Integer> out = new LinkedHashMap<>();
        COUNTERS.forEach((key, value) -> {
            if (value.get() > 0) {
                out.put(key, value.get());
            }
        });
        return out.isEmpty() ? null : out;
    }

    /** Called after FastStats accepted a send: counting starts over. */
    static void resetCounters() {
        COUNTERS.values().forEach(counter -> counter.set(0));
    }

    /** Ids of every mod registered with Mycel (MyCelium's and third-party). */
    static String[] mycelMods() {
        return Mycel.getRegisteredMods().stream().map(ModMetadata::id).filter(id -> !id.equals(Mycel.MOD_ID))
                .toArray(String[]::new);
    }

    /** Version of each registered mod. */
    static Map<String, String> modVersions() {
        Map<String, String> out = new LinkedHashMap<>();
        for (ModMetadata mod : Mycel.getRegisteredMods()) {
            if (!mod.id().equals(Mycel.MOD_ID)) {
                out.put(mod.id(), mod.version());
            }
        }
        return out;
    }

    /** Registered mods not made by MyCelium: who else builds on Mycel. */
    static String[] thirdPartyMods() {
        return Mycel.getRegisteredMods().stream()
                .filter(mod -> !mod.id().equals(Mycel.MOD_ID))
                .filter(mod -> mod.authors().stream().noneMatch(author -> author.equalsIgnoreCase("MyCelium")))
                .map(ModMetadata::id).toArray(String[]::new);
    }

    /** Registered mods that are turned off. */
    static String[] disabledMods() {
        return Mycel.getRegisteredMods().stream().map(ModMetadata::id).filter(id -> !Mycel.isEnabled(id))
                .toArray(String[]::new);
    }

    /** Registered mods with a newer version available (from the update checker). */
    static String[] outdatedMods() {
        List<String> out = new ArrayList<>();
        for (ModMetadata mod : Mycel.getRegisteredMods()) {
            UpdateResult result = UpdateChecker.lastResult(mod.id());
            if (result != null && result.updateAvailable()) {
                out.add(mod.id());
            }
        }
        return out.toArray(String[]::new);
    }

    /** How many settings each mod has changed from the default (how much people customize). */
    static Map<String, Integer> changedSettings() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (ModMetadata mod : Mycel.getRegisteredMods()) {
            MycelConfig config = Mycel.getConfig(mod.id());
            if (config == null) {
                continue;
            }
            int changed = 0;
            for (ConfigValue<?> value : config.values()) {
                if (!value.isDefault()) {
                    changed++;
                }
            }
            out.put(mod.id(), changed);
        }
        return out;
    }

    /** Names of the settings changed from their default, as {@code mod.setting} (popular tweaks). */
    static String[] changedSettingNames() {
        List<String> out = new ArrayList<>();
        for (ModMetadata mod : Mycel.getRegisteredMods()) {
            MycelConfig config = Mycel.getConfig(mod.id());
            if (config == null) {
                continue;
            }
            for (ConfigValue<?> value : config.values()) {
                if (!value.isDefault()) {
                    out.add(mod.id() + "." + value.key());
                }
            }
        }
        return out.stream().limit(100).toArray(String[]::new);
    }

    /** How many mods are installed in total. */
    static int installedMods() {
        return PlatformHolder.get().loadedMods().size();
    }

    /** Which of {@link #KNOWN_MODS} are installed. */
    static String[] knownMods() {
        List<String> out = new ArrayList<>();
        for (LoadedMod mod : PlatformHolder.get().loadedMods()) {
            String id = mod.id().toLowerCase(Locale.ROOT);
            if (KNOWN_MODS.contains(id)) {
                out.add(id);
            }
        }
        return out.toArray(String[]::new);
    }

    /** Maximum Java heap in whole gigabytes. */
    static long maxMemoryGb() {
        return Math.round(Runtime.getRuntime().maxMemory() / (1024.0 * 1024 * 1024));
    }
}

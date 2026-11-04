package my.celium.org.internal.stats;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.faststats.ErrorTracker;
import dev.faststats.Metrics;
import dev.faststats.config.SimpleConfig;
import dev.faststats.data.Metric;
import dev.faststats.internal.Logger;
import dev.faststats.internal.PlatformLoggerFactory;
import my.celium.org.Mycel;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.logging.MycelLog;
import my.celium.org.stats.ModStats;

/** Everything that touches FastStats classes. Only {@link MycelStats} calls it, inside try/catch. */
final class StatsRuntime {
    private static final String OWN_PACKAGE = "my.celium.org.";
    /** Dark Mode 1.0.x reports its own errors to its own project with its own FastStats copy. */
    private static final String DARK_MODE = "my.celium.org.darkmode.";
    private static final String SHADED = "my.celium.org.internal.faststats.";

    private static PlatformLoggerFactory loggers;
    private static SimpleConfig config;
    private static boolean client;
    private static boolean errors;
    private static MycelStatsContext context;
    private static ErrorTracker tracker;
    /** Contexts of mods using {@link ModStats}, and their error trackers. */
    private static final List<MycelStatsContext> MOD_CONTEXTS = new ArrayList<>();
    private static final Map<ModStats, ErrorTracker> MOD_TRACKERS = new ConcurrentHashMap<>();

    private StatsRuntime() {
    }

    static synchronized void start(String token, boolean onClient, boolean withErrors) {
        if (context != null) {
            return;
        }
        client = onClient;
        errors = withErrors;
        loggers = new PlatformLoggerFactory(StatsRuntime::print);
        Path file = PlatformHolder.get().configDir().resolve("faststats").resolve("config.properties");
        config = SimpleConfig.read(file, loggers, client);
        MycelStatsContext.Factory factory = factory(token, Mycel.MOD_ID, Mycel.getVersion());
        if (errors) {
            tracker = tracker(StatsRuntime::involvesMycel);
            factory.errorTrackerService(tracker);
        }
        factory.metrics(StatsRuntime::metrics);
        context = factory.create();
        context.ready();
        Runtime.getRuntime().addShutdownHook(new Thread(StatsRuntime::stop, "Mycel stats shutdown"));
    }

    /** Starts one mod's project with the settings Mycel started with. Needs {@link #start} first. */
    static synchronized void startMod(ModStats stats) {
        if (context == null) {
            return;
        }
        MycelStatsContext.Factory factory = factory(stats.token(), stats.mod().id(), stats.mod().version());
        if (errors && stats.errorPackage() != null) {
            String prefix = stats.errorPackage();
            ErrorTracker modTracker = tracker(error -> hasFrame(error, prefix));
            MOD_TRACKERS.put(stats, modTracker);
            factory.errorTrackerService(modTracker);
        }
        factory.metrics(metrics -> modMetrics(metrics, stats));
        MycelStatsContext modContext = factory.create();
        modContext.ready();
        MOD_CONTEXTS.add(modContext);
        stats.markRunning();
    }

    static synchronized void stop() {
        for (MycelStatsContext modContext : MOD_CONTEXTS) {
            modContext.shutdown();
        }
        MOD_CONTEXTS.clear();
        if (context != null) {
            context.shutdown();
            context = null;
        }
    }

    private static MycelStatsContext.Factory factory(String token, String project, String version) {
        return new MycelStatsContext.Factory(loggers, config,
                PlatformHolder.get().platform().name().toLowerCase(java.util.Locale.ROOT), token, client, project, version);
    }

    private static ErrorTracker tracker(java.util.function.Predicate<Throwable> wanted) {
        return ErrorTracker.contextAware()
                .addExclusionRule((thread, error) -> !wanted.test(error))
                .anonymize("(?i)(?:/home/|/Users/|[a-z]:\\\\Users\\\\)[^/\\\\\\s]+", "[user home]")
                .anonymize("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "[uuid]")
                .anonymize("\\b\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?\\b", "[ip]");
    }

    /** Hands {@code error} to the project of the mod it went through, else to Mycel's. */
    static void report(Throwable error) {
        for (Map.Entry<ModStats, ErrorTracker> entry : MOD_TRACKERS.entrySet()) {
            if (hasFrame(error, entry.getKey().errorPackage())) {
                entry.getValue().trackError(error);
                return;
            }
        }
        ErrorTracker current = tracker;
        if (current != null && involvesMycel(error)) {
            current.trackError(error);
        }
    }

    /** Hands {@code error} to one mod's project when it went through that mod. */
    static void reportFor(ModStats stats, Throwable error) {
        ErrorTracker modTracker = MOD_TRACKERS.get(stats);
        if (modTracker != null && hasFrame(error, stats.errorPackage())) {
            modTracker.trackError(error);
        }
    }

    /** Errors through MyCelium code that no mod with its own project claims. */
    static boolean involvesMycel(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            for (StackTraceElement frame : t.getStackTrace()) {
                String name = frame.getClassName();
                if (name.startsWith(OWN_PACKAGE) && !name.startsWith(DARK_MODE) && !name.startsWith(SHADED)
                        && !claimedByMod(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean claimedByMod(String className) {
        for (ModStats stats : MOD_TRACKERS.keySet()) {
            if (className.startsWith(stats.errorPackage())) {
                return true;
            }
        }
        return false;
    }

    static boolean hasFrame(Throwable error, String prefix) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            for (StackTraceElement frame : t.getStackTrace()) {
                if (frame.getClassName().startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Metrics metrics(Metrics.Factory factory) {
        return factory
                .addMetric(Metric.stringArray("mycel_mods", StatsData::mycelMods))
                .addMetric(Metric.stringMap("mod_versions", StatsData::modVersions))
                .addMetric(Metric.number("mycel_mod_count", () -> StatsData.mycelMods().length))
                .addMetric(Metric.stringArray("third_party_mods", StatsData::thirdPartyMods))
                .addMetric(Metric.stringArray("disabled_mods", StatsData::disabledMods))
                .addMetric(Metric.stringArray("outdated_mods", StatsData::outdatedMods))
                .addMetric(Metric.numberMap("changed_settings", StatsData::changedSettings))
                .addMetric(Metric.stringArray("changed_setting_names", StatsData::changedSettingNames))
                .addMetric(Metric.number("installed_mods", StatsData::installedMods))
                .addMetric(Metric.stringArray("known_mods", StatsData::knownMods))
                .addMetric(Metric.number("max_memory_gb", StatsData::maxMemoryGb))
                .addMetric(Metric.bool("update_checker", MycelStats::updateCheckerOn))
                .addMetric(Metric.bool("dump_uploads", MycelStats::dumpUploadsOn))
                .addMetric(Metric.string("game_language", MycelStats::language))
                .addMetric(Metric.numberMap("usage", StatsData::counters))
                .onFlush(StatsData::resetCounters)
                .create();
    }

    @SuppressWarnings("unchecked")
    private static Metrics modMetrics(Metrics.Factory factory, ModStats stats) {
        for (ModStats.Entry entry : stats.metrics()) {
            java.util.function.Supplier<?> source = entry.supplier();
            String id = entry.id();
            Metric<?> metric = switch (entry.kind()) {
                case NUMBER -> Metric.number(id, () -> (Number) source.get());
                case STRING -> Metric.string(id, () -> (String) source.get());
                case BOOL -> Metric.bool(id, () -> (Boolean) source.get());
                case STRING_ARRAY -> Metric.stringArray(id, () -> (String[]) source.get());
                case NUMBER_MAP -> Metric.numberMap(id, () -> (Map<String, ? extends Number>) source.get());
                case STRING_MAP -> Metric.stringMap(id, () -> (Map<String, String>) source.get());
            };
            factory.addMetric(metric);
        }
        for (String id : stats.counterIds()) {
            factory.addMetric(Metric.numberMap(id, () -> stats.counters(id)));
        }
        return factory.addMetric(Metric.numberMap("usage", stats::counters))
                .onFlush(stats::resetCounters)
                .create();
    }

    private static void print(Logger.LogLevel level, Throwable error, String message) {
        switch (level) {
            case ERROR -> MycelLog.warn("FastStats: {}", message);
            case WARN -> MycelLog.debug("FastStats: {}", message);
            default -> MycelLog.info("FastStats: {}", message);
        }
    }
}

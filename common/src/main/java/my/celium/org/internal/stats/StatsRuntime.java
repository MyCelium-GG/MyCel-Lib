package my.celium.org.internal.stats;

import java.nio.file.Path;

import dev.faststats.ErrorTracker;
import dev.faststats.Metrics;
import dev.faststats.config.SimpleConfig;
import dev.faststats.data.Metric;
import dev.faststats.internal.Logger;
import dev.faststats.internal.PlatformLoggerFactory;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.logging.MycelLog;

/** Everything that touches FastStats classes. Only {@link MycelStats} calls it, inside try/catch. */
final class StatsRuntime {
    private static final String OWN_PACKAGE = "my.celium.org.";
    /** Dark Mode reports its own errors to its own project. */
    private static final String OWN_REPORTER = "my.celium.org.darkmode.";

    private static MycelStatsContext context;
    private static ErrorTracker tracker;

    private StatsRuntime() {
    }

    static synchronized void start(String token, boolean client, boolean errors) {
        if (context != null) {
            return;
        }
        PlatformLoggerFactory loggers = new PlatformLoggerFactory(StatsRuntime::print);
        Path file = PlatformHolder.get().configDir().resolve("faststats").resolve("config.properties");
        SimpleConfig config = SimpleConfig.read(file, loggers, client);
        MycelStatsContext.Factory factory = new MycelStatsContext.Factory(loggers, config,
                PlatformHolder.get().platform().name().toLowerCase(java.util.Locale.ROOT), token, client);
        if (errors) {
            tracker = ErrorTracker.contextAware()
                    .addExclusionRule((thread, error) -> !involvesMyCelium(error))
                    .anonymize("(?i)(?:/home/|/Users/|[a-z]:\\\\Users\\\\)[^/\\\\\\s]+", "[user home]")
                    .anonymize("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "[uuid]")
                    .anonymize("\\b\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?\\b", "[ip]");
            factory.errorTrackerService(tracker);
        }
        factory.metrics(StatsRuntime::metrics);
        context = factory.create();
        context.ready();
        Runtime.getRuntime().addShutdownHook(new Thread(StatsRuntime::stop, "Mycel stats shutdown"));
    }

    static synchronized void stop() {
        if (context != null) {
            context.shutdown();
            context = null;
        }
    }

    static void report(Throwable error) {
        ErrorTracker current = tracker;
        if (current != null && involvesMyCelium(error)) {
            current.trackError(error);
        }
    }

    static boolean involvesMyCelium(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            for (StackTraceElement frame : t.getStackTrace()) {
                String name = frame.getClassName();
                if (name.startsWith(OWN_PACKAGE) && !name.startsWith(OWN_REPORTER)
                        && !name.startsWith("my.celium.org.internal.faststats.")) {
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

    private static void print(Logger.LogLevel level, Throwable error, String message) {
        switch (level) {
            case ERROR -> MycelLog.warn("FastStats: {}", message);
            case WARN -> MycelLog.debug("FastStats: {}", message);
            default -> MycelLog.info("FastStats: {}", message);
        }
    }
}

package my.celium.org.internal.stats;

import my.celium.org.config.ConfigValue;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.logging.MycelLog;

/**
 * Entry point for Mycel's anonymous stats. Safe to call anywhere: every FastStats class sits
 * behind {@link StatsRuntime}, and any failure (a missing class in a dev setup, a network error)
 * only turns stats off.
 *
 * <p>Clients: nothing starts until the player answered the consent screen once
 * ({@code statsAsked}), and then only if they agreed ({@code shareStats}). Dedicated servers:
 * on unless {@code shareStats} is off; FastStats prints a notice and sends nothing on the very
 * first start. The shared {@code config/faststats/config.properties} and
 * {@code -Dfaststats.enabled=false} turn it off too.
 *
 * @hidden
 */
public final class MycelStats {
    /** Project token from faststats.dev. */
    static final String TOKEN = "a902a8b98f2c0b05b120b6a492f8912c";

    private static volatile ConfigValue<Boolean> shareStats;
    private static volatile ConfigValue<Boolean> shareErrors;
    private static volatile ConfigValue<Boolean> asked;
    private static volatile ConfigValue<Boolean> updateChecker;
    private static volatile ConfigValue<Boolean> dumpUploads;
    private static volatile boolean running;

    private MycelStats() {
    }

    /**
     * Called once from {@code Mycel.initialize()} with Mycel's own settings.
     *
     * @param share         {@code shareStats}
     * @param errors        {@code shareErrors}
     * @param askedFlag     {@code statsAsked}
     * @param updates       {@code enableUpdateChecker}, reported as a metric
     * @param uploads       {@code allowDumpUploads}, reported as a metric
     */
    public static void init(ConfigValue<Boolean> share, ConfigValue<Boolean> errors, ConfigValue<Boolean> askedFlag,
            ConfigValue<Boolean> updates, ConfigValue<Boolean> uploads) {
        shareStats = share;
        shareErrors = errors;
        asked = askedFlag;
        updateChecker = updates;
        dumpUploads = uploads;
        if (!PlatformHolder.get().isClient()) {
            if (share.get()) {
                MycelLog.info("Mycel shares anonymous usage stats with the MyCelium team. Turn it off with "
                        + "shareStats=false in config/mycel.json. What is sent: https://wiki.mycelium-gg.org/privacy#mycel-stats");
                start(false);
            }
            return;
        }
        if (askedFlag.get()) {
            if (share.get()) {
                start(true);
            }
        } else {
            askOnClient();
        }
    }

    /** Whether the client still has to show the consent screen. */
    public static boolean needsConsent() {
        ConfigValue<Boolean> flag = asked;
        return flag != null && !flag.get();
    }

    /**
     * The player's answer on the consent screen. Saves it and starts sending when they agreed.
     *
     * @param share  send stats
     * @param errors also send error reports
     */
    public static void answer(boolean share, boolean errors) {
        if (asked == null) {
            return;
        }
        shareStats.set(share);
        shareErrors.set(share && errors);
        asked.set(true);
        my.celium.org.config.MycelConfig config = my.celium.org.Mycel.getConfig(my.celium.org.Mycel.MOD_ID);
        if (config != null) {
            config.save();
        }
        if (share) {
            start(true);
        }
    }

    private static void askOnClient() {
        try {
            Class.forName("my.celium.org.client.StatsConsent").getMethod("watch").invoke(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            MycelLog.debug("Stats consent screen unavailable: {}", e.toString());
        }
    }

    private static synchronized void start(boolean client) {
        if (running) {
            return;
        }
        try {
            StatsRuntime.start(TOKEN, client, shareErrors != null && shareErrors.get());
            running = true;
        } catch (LinkageError | RuntimeException e) {
            MycelLog.debug("Mycel stats are off: {}", e.toString());
        }
    }

    /**
     * Hands an error to FastStats when error reports are on and the error went through MyCelium code.
     *
     * @param error the error
     */
    public static void report(Throwable error) {
        if (!running || error == null) {
            return;
        }
        try {
            StatsRuntime.report(error);
        } catch (LinkageError | RuntimeException ignored) {
            // Reporting must never cause problems of its own.
        }
    }

    /**
     * Counts one use of something, e.g. {@code command_dump}.
     *
     * @param what lowercase letters and underscores
     */
    public static void count(String what) {
        if (running) {
            StatsData.count(what);
        }
    }

    static boolean updateCheckerOn() {
        return updateChecker == null || updateChecker.get();
    }

    static boolean dumpUploadsOn() {
        return dumpUploads == null || dumpUploads.get();
    }

    /** The game language on clients (like {@code de_de}), or {@code null} on servers. */
    static String language() {
        if (!PlatformHolder.get().isClient()) {
            return null;
        }
        try {
            Object value = Class.forName("my.celium.org.client.StatsConsent").getMethod("language").invoke(null);
            return value instanceof String code && code.matches("[a-z]{2,3}_[a-z0-9]{2,4}") ? code : null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return null;
        }
    }
}

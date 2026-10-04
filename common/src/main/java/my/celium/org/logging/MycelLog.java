package my.celium.org.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single logging entry point for Mycel and dependent mods.
 *
 * <p>All output uses the {@code [Mycel]} marker so server administrators can
 * filter it. Startup logging is deliberately quiet: one line per registered
 * mod at most, plus warnings/errors with actionable context.
 *
 * <p>Mod-scoped variants are deliberately named {@code *Mod} (rather than
 * overloads) so a message containing {@code {}} can never be misrouted to the
 * wrong signature.
 */
public final class MycelLog {
    private static final Logger LOG = LoggerFactory.getLogger("Mycel");

    private MycelLog() {
    }

    public static void info(String message, Object... args) {
        LOG.info("[Mycel] " + message, args);
    }

    public static void infoMod(String modId, String message, Object... args) {
        LOG.info("[Mycel/{}] {}", modId, format(message, args));
    }

    public static void warn(String message, Object... args) {
        LOG.warn("[Mycel] " + message, args);
    }

    public static void warnMod(String modId, String message, Object... args) {
        LOG.warn("[Mycel/{}] {}", modId, format(message, args));
    }

    public static void error(String message, Object... args) {
        LOG.error("[Mycel] " + message, args);
    }

    public static void errorMod(String modId, String message, Object... args) {
        LOG.error("[Mycel/{}] {}", modId, format(message, args));
    }

    public static void errorMod(String modId, String message, Throwable cause) {
        LOG.error("[Mycel/{}] {}", modId, message, cause);
    }

    public static void debug(String message, Object... args) {
        LOG.debug("[Mycel] " + message, args);
    }

    /** Minimal {@code {}} formatter for mod-scoped messages. */
    static String format(String message, Object... args) {
        if (args == null || args.length == 0 || message == null) {
            return String.valueOf(message);
        }
        StringBuilder out = new StringBuilder(message.length() + 16);
        int arg = 0;
        int cursor = 0;
        while (true) {
            int next = message.indexOf("{}", cursor);
            if (next < 0 || arg >= args.length) {
                out.append(message, cursor, message.length());
                break;
            }
            out.append(message, cursor, next);
            out.append(String.valueOf(args[arg++]));
            cursor = next + 2;
        }
        return out.toString();
    }
}

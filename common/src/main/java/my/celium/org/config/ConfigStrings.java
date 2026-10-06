package my.celium.org.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * String conversion for {@link ConfigValue}s: display, parsing and lookup by
 * path.
 *
 * <p>A path is {@code "key"} for root values or {@code "category.key"} for
 * categorised ones. Bare-key lookup succeeds when exactly one value carries
 * that key; otherwise the fully qualified path is required.
 *
 * <p>All parsing goes through the same validators as programmatic
 * {@link ConfigValue#set(Object)} calls, so {@code /mycel set} can never sneak
 * an out-of-range value past the config screen's rules.
 */
public final class ConfigStrings {
    private ConfigStrings() {
    }

    /** Canonical path of a value: {@code key} or {@code category.key}. */
    public static String pathOf(ConfigValue<?> value) {
        Objects.requireNonNull(value, "value");
        return value.category().isEmpty() ? value.key() : value.category() + "." + value.key();
    }

    /**
     * Finds a value inside {@code config} by path (see class javadoc).
     *
     * @return the value, or {@code null} when nothing matches
     */
    public static ConfigValue<?> find(MycelConfig config, String path) {
        Objects.requireNonNull(config, "config");
        if (path == null || path.isBlank()) {
            return null;
        }
        String query = path.trim();
        if (query.contains(".")) {
            // Exact qualified match.
            for (ConfigValue<?> value : config.values()) {
                if (pathOf(value).equalsIgnoreCase(query)) {
                    return value;
                }
            }
            return null;
        }
        // Bare-key match: succeeds only when exactly one value carries that key.
        ConfigValue<?> match = null;
        for (ConfigValue<?> value : config.values()) {
            if (value.key().equalsIgnoreCase(query)) {
                if (match != null) {
                    return null; // ambiguous: present in several categories
                }
                match = value;
            }
        }
        return match;
    }

    /** Human-readable current value (lists render comma-separated). */
    public static String valueToString(ConfigValue<?> value) {
        Objects.requireNonNull(value, "value");
        Object current = value.get();
        if (current instanceof List<?> list) {
            List<String> parts = new ArrayList<>();
            for (Object entry : list) {
                parts.add(String.valueOf(entry));
            }
            return String.join(", ", parts);
        }
        return String.valueOf(current);
    }

    /** Human-readable default value. */
    public static String defaultToString(ConfigValue<?> value) {
        Objects.requireNonNull(value, "value");
        Object def = value.getDefault();
        if (def instanceof List<?> list) {
            List<String> parts = new ArrayList<>();
            for (Object entry : list) {
                parts.add(String.valueOf(entry));
            }
            return String.join(", ", parts);
        }
        return String.valueOf(def);
    }

    /** Short constraint hint: ranges, allowed enum names, max length, ... */
    public static String constraintHint(ConfigValue<?> value) {
        Objects.requireNonNull(value, "value");
        if (value instanceof ConfigValue.IntegerValue v) {
            return "[" + v.min() + ".." + v.max() + "]";
        }
        if (value instanceof ConfigValue.LongValue v) {
            return "[" + v.min() + ".." + v.max() + "]";
        }
        if (value instanceof ConfigValue.DoubleValue v) {
            return "[" + v.min() + ".." + v.max() + "]";
        }
        if (value instanceof ConfigValue.BooleanValue) {
            return "true/false";
        }
        if (value instanceof ConfigValue.StringValue v) {
            return v.maxLength() >= Integer.MAX_VALUE / 2 ? "text"
                    : "max " + v.maxLength() + " chars";
        }
        if (value instanceof ConfigValue.EnumValue<?> v) {
            Object[] constants = v.type().getEnumConstants();
            List<String> names = new ArrayList<>();
            for (Object constant : constants) {
                names.add(String.valueOf(constant));
            }
            return String.join(" | ", names);
        }
        if (value instanceof ConfigValue.StringListValue) {
            return "comma-separated, empty clears";
        }
        return value.kind().name().toLowerCase(Locale.ROOT);
    }

    /** One-line type summary used by {@code /mycel get <mod>}. */
    public static String describe(ConfigValue<?> value) {
        return value.kind().name().toLowerCase(Locale.ROOT) + " " + constraintHint(value);
    }

    /**
     * Parses {@code text} and applies it to {@code value}, exactly as
     * {@link ConfigValue#set(Object)} would.
     *
     * @throws IllegalArgumentException with a friendly message when invalid
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void parseAndSet(ConfigValue<?> value, String text) {
        Objects.requireNonNull(value, "value");
        if (text == null) {
            throw new IllegalArgumentException("Missing value");
        }
        String input = text.trim();
        try {
            if (value instanceof ConfigValue.BooleanValue typed) {
                typed.set(parseBoolean(input));
                return;
            }
            if (value instanceof ConfigValue.IntegerValue typed) {
                typed.set(typed.validate(Integer.parseInt(input)));
                return;
            }
            if (value instanceof ConfigValue.LongValue typed) {
                typed.set(typed.validate(Long.parseLong(input)));
                return;
            }
            if (value instanceof ConfigValue.DoubleValue typed) {
                typed.set(typed.validate(Double.parseDouble(input)));
                return;
            }
            if (value instanceof ConfigValue.StringValue typed) {
                typed.set(typed.validate(input));
                return;
            }
            if (value instanceof ConfigValue.EnumValue typed) {
                typed.set(parseEnum(typed, input));
                return;
            }
            if (value instanceof ConfigValue.StringListValue typed) {
                typed.set(parseStringList(input));
                return;
            }
            throw new IllegalArgumentException("Unsupported value type: " + value.kind());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Not a " + value.kind().name().toLowerCase(Locale.ROOT)
                            + " (" + constraintHint(value) + "): '" + text + "'",
                    e);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && !e.getMessage().isBlank()) {
                throw e;
            }
            throw new IllegalArgumentException("Invalid value '" + text + "' (" + constraintHint(value) + ")", e);
        }
    }

    private static Boolean parseBoolean(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "true", "1", "yes", "on" -> Boolean.TRUE;
            case "false", "0", "no", "off" -> Boolean.FALSE;
            default -> throw new IllegalArgumentException("Expected true/false (also 1/0, yes/no, on/off)");
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseEnum(ConfigValue.EnumValue typed, String input) {
        for (Object constant : typed.type().getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(input.trim())) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Unknown option '" + input + "' (" + constraintHint(typed) + ")");
    }

    private static List<String> parseStringList(String input) {
        if (input.isBlank()) {
            return List.of();
        }
        List<String> entries = new ArrayList<>();
        for (String part : input.split(",", -1)) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                entries.add(trimmed);
            }
        }
        return List.copyOf(entries);
    }
}

package my.celium.org.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A single typed configuration value.
 *
 * <p>Values are created through {@link ConfigBuilder} and owned by a
 * {@link MycelConfig}. Reads are cheap and thread-safe. Writes validate,
 * notify listeners and persist (debounced by the owning config).
 *
 * <p>Programmatic {@link #set(Object)} calls with invalid data throw
 * {@link IllegalArgumentException}; data loaded from disk is clamped or
 * reset to the default with a warning instead, so a hand-edited file can
 * never crash the game.
 *
 * @param <T> value type
 */
public abstract class ConfigValue<T> {
    private final String category;
    private final String key;
    private final String description;
    private final T defaultValue;
    private volatile T current;
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

    protected ConfigValue(String category, String key, T defaultValue, String description) {
        this.category = category == null ? "" : category;
        this.key = Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Config key must not be blank");
        }
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.current = defaultValue;
        this.description = description == null ? "" : description;
    }

    /** Category, or {@code ""} for the root level. */
    public String category() {
        return category;
    }

    public String key() {
        return key;
    }

    /** Human readable explanation shown in the config screen. */
    public String description() {
        return description;
    }

    public abstract ValueKind kind();

    public T get() {
        return current;
    }

    public T getDefault() {
        return defaultValue;
    }

    public boolean isDefault() {
        return Objects.equals(current, defaultValue);
    }

    /**
     * Sets a new value after validation. Persists and notifies listeners
     * when the value actually changed.
     *
     * @throws IllegalArgumentException when the value is invalid
     */
    public void set(T value) {
        T checked = check(Objects.requireNonNull(value, "value"));
        T old = current;
        if (Objects.equals(old, checked)) {
            return;
        }
        current = checked;
        onChanged(old, checked);
    }

    /** Resets to the default value (persists + notifies when changed). */
    public void reset() {
        set(defaultValue);
    }

    /** Registers a change listener; fired on the thread that performed the change. */
    public void onChange(Consumer<T> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /** Applies a raw value loaded from disk; invalid data falls back to the default. */
    void loadRaw(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            current = defaultValue;
            return;
        }
        try {
            current = check(deserialize(element));
        } catch (RuntimeException e) {
            current = defaultValue;
        }
    }

    JsonElement saveRaw() {
        return serialize(current);
    }

    /** Validates (and possibly normalises) a value; throws when unacceptable for code-driven sets. */
    protected abstract T check(T value);

    /**
     * Validates a candidate exactly as {@link #set(Object)} would, without
     * changing anything. Used by the config screen's live validation.
     *
     * @throws IllegalArgumentException when the value is invalid
     */
    public T validate(T value) {
        return check(Objects.requireNonNull(value, "value"));
    }

    protected abstract JsonElement serialize(T value);

    protected abstract T deserialize(JsonElement element);

    private void onChanged(T oldValue, T newValue) {
        MycelConfig owner = owner();
        if (owner != null) {
            owner.markDirty();
        }
        for (Consumer<T> listener : listeners) {
            try {
                listener.accept(newValue);
            } catch (RuntimeException e) {
                my.celium.org.logging.MycelLog.warn("Config listener for '{}.{}' threw; ignoring",
                        displayPath(), e.toString());
            }
        }
    }

    /** Owning config; wired by {@link MycelConfig} after {@code build()}. */
    private volatile MycelConfig owner;

    void attach(MycelConfig config) {
        this.owner = config;
    }

    private MycelConfig owner() {
        return owner;
    }

    private String displayPath() {
        return category.isEmpty() ? key : category + "." + key;
    }

    // ------------------------------------------------------------------ impls

    /** Boolean value. */
    public static final class BooleanValue extends ConfigValue<Boolean> {
        public BooleanValue(String category, String key, boolean defaultValue, String description) {
            super(category, key, defaultValue, description);
        }

        @Override
        public ValueKind kind() {
            return ValueKind.BOOLEAN;
        }

        @Override
        protected Boolean check(Boolean value) {
            return value;
        }

        @Override
        protected JsonElement serialize(Boolean value) {
            return new JsonPrimitive(value);
        }

        @Override
        protected Boolean deserialize(JsonElement element) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            }
            // Tolerate 0/1 and "true"/"false" from hand edits.
            String text = element.getAsString();
            if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Not a boolean: " + element);
        }
    }

    /** Integer value with an inclusive range. */
    public static final class IntegerValue extends ConfigValue<Integer> {
        private final int min;
        private final int max;

        public IntegerValue(String category, String key, int defaultValue, int min, int max, String description) {
            super(category, key, defaultValue, description);
            if (min > max) {
                throw new IllegalArgumentException("min > max for '" + key + "'");
            }
            if (defaultValue < min || defaultValue > max) {
                throw new IllegalArgumentException("Default out of range for '" + key + "'");
            }
            this.min = min;
            this.max = max;
        }

        public int min() {
            return min;
        }

        public int max() {
            return max;
        }

        @Override
        public ValueKind kind() {
            return ValueKind.INTEGER;
        }

        @Override
        protected Integer check(Integer value) {
            if (value < min || value > max) {
                throw new IllegalArgumentException(
                        "Value " + value + " out of range [" + min + ", " + max + "]");
            }
            return value;
        }

        @Override
        void loadRaw(JsonElement element) {
            // Disk data is clamped, never rejected.
            try {
                int parsed = deserializeLenient(element);
                super.current = Math.min(max, Math.max(min, parsed));
            } catch (RuntimeException e) {
                super.current = getDefault();
            }
        }

        @Override
        protected JsonElement serialize(Integer value) {
            return new JsonPrimitive(value);
        }

        @Override
        protected Integer deserialize(JsonElement element) {
            return check(deserializeLenient(element));
        }

        private int deserializeLenient(JsonElement element) {
            try {
                return element.getAsInt();
            } catch (RuntimeException e) {
                return Integer.parseInt(element.getAsString().trim());
            }
        }
    }

    /** Long value with an inclusive range. */
    public static final class LongValue extends ConfigValue<Long> {
        private final long min;
        private final long max;

        public LongValue(String category, String key, long defaultValue, long min, long max, String description) {
            super(category, key, defaultValue, description);
            if (min > max) {
                throw new IllegalArgumentException("min > max for '" + key + "'");
            }
            if (defaultValue < min || defaultValue > max) {
                throw new IllegalArgumentException("Default out of range for '" + key + "'");
            }
            this.min = min;
            this.max = max;
        }

        public long min() {
            return min;
        }

        public long max() {
            return max;
        }

        @Override
        public ValueKind kind() {
            return ValueKind.LONG;
        }

        @Override
        protected Long check(Long value) {
            if (value < min || value > max) {
                throw new IllegalArgumentException(
                        "Value " + value + " out of range [" + min + ", " + max + "]");
            }
            return value;
        }

        @Override
        void loadRaw(JsonElement element) {
            try {
                long parsed = deserializeLenient(element);
                super.current = Math.min(max, Math.max(min, parsed));
            } catch (RuntimeException e) {
                super.current = getDefault();
            }
        }

        @Override
        protected JsonElement serialize(Long value) {
            return new JsonPrimitive(value);
        }

        @Override
        protected Long deserialize(JsonElement element) {
            return check(deserializeLenient(element));
        }

        private long deserializeLenient(JsonElement element) {
            try {
                return element.getAsLong();
            } catch (RuntimeException e) {
                return Long.parseLong(element.getAsString().trim());
            }
        }
    }

    /** Double value with an inclusive range. */
    public static final class DoubleValue extends ConfigValue<Double> {
        private final double min;
        private final double max;

        public DoubleValue(String category, String key, double defaultValue, double min, double max,
                String description) {
            super(category, key, defaultValue, description);
            if (!(min <= max)) {
                throw new IllegalArgumentException("min > max for '" + key + "'");
            }
            if (!(defaultValue >= min && defaultValue <= max)) {
                throw new IllegalArgumentException("Default out of range for '" + key + "'");
            }
            this.min = min;
            this.max = max;
        }

        public double min() {
            return min;
        }

        public double max() {
            return max;
        }

        @Override
        public ValueKind kind() {
            return ValueKind.DOUBLE;
        }

        @Override
        protected Double check(Double value) {
            if (Double.isNaN(value) || value < min || value > max) {
                throw new IllegalArgumentException(
                        "Value " + value + " out of range [" + min + ", " + max + "]");
            }
            return value;
        }

        @Override
        void loadRaw(JsonElement element) {
            try {
                double parsed = deserializeLenient(element);
                if (Double.isNaN(parsed)) {
                    super.current = getDefault();
                } else {
                    super.current = Math.min(max, Math.max(min, parsed));
                }
            } catch (RuntimeException e) {
                super.current = getDefault();
            }
        }

        @Override
        protected JsonElement serialize(Double value) {
            return new JsonPrimitive(value);
        }

        @Override
        protected Double deserialize(JsonElement element) {
            return check(deserializeLenient(element));
        }

        private double deserializeLenient(JsonElement element) {
            try {
                return element.getAsDouble();
            } catch (RuntimeException e) {
                return Double.parseDouble(element.getAsString().trim());
            }
        }
    }

    /** String value with a maximum length. */
    public static final class StringValue extends ConfigValue<String> {
        private final int maxLength;

        public StringValue(String category, String key, String defaultValue, int maxLength, String description) {
            super(category, key, defaultValue, description);
            if (maxLength < 0) {
                throw new IllegalArgumentException("maxLength must be >= 0");
            }
            if (defaultValue.length() > maxLength) {
                throw new IllegalArgumentException("Default longer than maxLength for '" + key + "'");
            }
            this.maxLength = maxLength;
        }

        public int maxLength() {
            return maxLength;
        }

        @Override
        public ValueKind kind() {
            return ValueKind.STRING;
        }

        @Override
        protected String check(String value) {
            if (value.length() > maxLength) {
                throw new IllegalArgumentException(
                        "String longer than maxLength " + maxLength + " for config value");
            }
            return value;
        }

        @Override
        void loadRaw(JsonElement element) {
            try {
                String text = element.getAsString();
                super.current = text.length() > maxLength ? text.substring(0, maxLength) : text;
            } catch (RuntimeException e) {
                super.current = getDefault();
            }
        }

        @Override
        protected JsonElement serialize(String value) {
            return new JsonPrimitive(value);
        }

        @Override
        protected String deserialize(JsonElement element) {
            return check(element.getAsString());
        }
    }

    /** Enum value; unknown names on disk fall back to the default. */
    public static final class EnumValue<E extends Enum<E>> extends ConfigValue<E> {
        private final Class<E> type;

        public EnumValue(String category, String key, Class<E> type, E defaultValue, String description) {
            super(category, key, defaultValue, description);
            this.type = Objects.requireNonNull(type, "type");
        }

        public Class<E> type() {
            return type;
        }

        /** All allowed constants, in declaration order. */
        public List<E> constants() {
            List<E> list = new ArrayList<>();
            Collections.addAll(list, type.getEnumConstants());
            return Collections.unmodifiableList(list);
        }

        @Override
        public ValueKind kind() {
            return ValueKind.ENUM;
        }

        @Override
        protected E check(E value) {
            return value;
        }

        @Override
        protected JsonElement serialize(E value) {
            return new JsonPrimitive(value.name());
        }

        @Override
        protected E deserialize(JsonElement element) {
            String name = element.getAsString().trim();
            for (E constant : type.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(name)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " constant: " + name);
        }
    }

    /** List-of-strings value (defensive copies in and out). */
    public static final class StringListValue extends ConfigValue<List<String>> {
        public StringListValue(String category, String key, List<String> defaultValue, String description) {
            super(category, key, List.copyOf(defaultValue), description);
        }

        @Override
        public ValueKind kind() {
            return ValueKind.STRING_LIST;
        }

        @Override
        public List<String> get() {
            return List.copyOf(super.get());
        }

        @Override
        public List<String> getDefault() {
            return List.copyOf(super.getDefault());
        }

        @Override
        protected List<String> check(List<String> value) {
            return List.copyOf(value);
        }

        @Override
        protected JsonElement serialize(List<String> value) {
            JsonArray array = new JsonArray();
            for (String entry : value) {
                array.add(entry);
            }
            return array;
        }

        @Override
        protected List<String> deserialize(JsonElement element) {
            if (!element.isJsonArray()) {
                throw new IllegalArgumentException("Expected a JSON array");
            }
            List<String> list = new ArrayList<>();
            for (JsonElement entry : element.getAsJsonArray()) {
                list.add(entry.getAsString());
            }
            return List.copyOf(list);
        }
    }
}

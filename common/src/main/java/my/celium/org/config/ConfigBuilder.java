package my.celium.org.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.google.gson.JsonObject;

import my.celium.org.logging.MycelLog;
import my.celium.org.metadata.ModMetadata;

/**
 * Fluent builder for a mod's configuration.
 *
 * <p>Typical use (about ten lines):
 * <pre>{@code
 * ConfigBuilder builder = Mycel.config(META);
 * ConfigValue<Boolean> enabled = builder.booleanValue("enabled", true, "Whether the feature runs.");
 * ConfigValue<Integer> radius = builder.intValue("radius", 8, 1, 32, "Search radius in blocks.");
 * MycelConfig config = builder.build();
 * }</pre>
 *
 * <p>Call {@link #category(String)} to scope subsequent values into a named
 * section (stored as a nested JSON object). Call {@link #root()} to return to
 * the top level. A builder is single-use: {@link #build()} may be called once.
 */
public final class ConfigBuilder {
    private final ModMetadata metadata;
    private final List<ConfigValue<?>> values = new ArrayList<>();
    private final Map<Integer, Consumer<JsonObject>> migrations = new LinkedHashMap<>();
    private String category = "";
    private int schemaVersion = 1;
    private boolean built;

    /** Creates a builder; prefer {@code Mycel.config(...)}. */
    public ConfigBuilder(ModMetadata metadata) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
    }

    /** Scopes subsequently defined values into {@code name} (letters, digits, {@code _-} only). */
    public ConfigBuilder category(String name) {
        Objects.requireNonNull(name, "name");
        if (!name.matches("[A-Za-z0-9_\\-]+")) {
            throw new IllegalArgumentException("Invalid category name: " + name);
        }
        this.category = name;
        return this;
    }

    /** Returns to the root level. */
    public ConfigBuilder root() {
        this.category = "";
        return this;
    }

    /** Declares the schema version of this config; defaults to {@code 1}. */
    public ConfigBuilder schemaVersion(int version) {
        if (version < 1) {
            throw new IllegalArgumentException("schemaVersion must be >= 1");
        }
        this.schemaVersion = version;
        return this;
    }

    /**
     * Registers a migration applied to the raw file content when upgrading
     * <em>from</em> {@code fromVersion}. Migrations run in ascending version
     * order before values are parsed, so old files keep working.
     */
    public ConfigBuilder migrate(int fromVersion, Consumer<JsonObject> migration) {
        if (fromVersion < 1) {
            throw new IllegalArgumentException("fromVersion must be >= 1");
        }
        migrations.put(fromVersion, Objects.requireNonNull(migration, "migration"));
        return this;
    }

    public ConfigValue<Boolean> booleanValue(String key, boolean defaultValue, String description) {
        return add(new ConfigValue.BooleanValue(category, key, defaultValue, description));
    }

    public ConfigValue<Integer> intValue(String key, int defaultValue, int min, int max, String description) {
        return add(new ConfigValue.IntegerValue(category, key, defaultValue, min, max, description));
    }

    public ConfigValue<Long> longValue(String key, long defaultValue, long min, long max, String description) {
        return add(new ConfigValue.LongValue(category, key, defaultValue, min, max, description));
    }

    public ConfigValue<Double> doubleValue(String key, double defaultValue, double min, double max,
            String description) {
        return add(new ConfigValue.DoubleValue(category, key, defaultValue, min, max, description));
    }

    public ConfigValue<String> stringValue(String key, String defaultValue, String description) {
        return stringValue(key, defaultValue, 512, description);
    }

    public ConfigValue<String> stringValue(String key, String defaultValue, int maxLength, String description) {
        return add(new ConfigValue.StringValue(category, key, defaultValue, maxLength, description));
    }

    public <E extends Enum<E>> ConfigValue<E> enumValue(String key, Class<E> type, E defaultValue,
            String description) {
        return add(new ConfigValue.EnumValue<>(category, key, type, defaultValue, description));
    }

    public ConfigValue<List<String>> stringListValue(String key, List<String> defaultValue, String description) {
        return add(new ConfigValue.StringListValue(category, key, defaultValue, description));
    }

    private <T, V extends ConfigValue<T>> V add(V value) {
        if (built) {
            throw new IllegalStateException("ConfigBuilder has already been built");
        }
        for (ConfigValue<?> existing : values) {
            if (existing.category().equals(value.category()) && existing.key().equals(value.key())) {
                throw new IllegalArgumentException("Duplicate config value: '" + value.key()
                        + "' in category '" + value.category() + "'");
            }
        }
        values.add(value);
        return value;
    }

    /** Finalises the config: loads {@code config/<modid>.json} and returns the live handle. */
    public MycelConfig build() {
        if (built) {
            throw new IllegalStateException("ConfigBuilder has already been built");
        }
        built = true;
        MycelConfig config = new MycelConfig(metadata, List.copyOf(values), schemaVersion,
                new LinkedHashMap<>(migrations));
        my.celium.org.Mycel.registerConfig(config);
        try {
            config.load();
        } catch (RuntimeException e) {
            MycelLog.warnMod(metadata.id(), "Failed to load config, using defaults: {}", e.toString());
        }
        return config;
    }
}

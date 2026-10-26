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

    /**
     * Creates a builder; prefer {@code Mycel.config(...)}.
     *
     * @param metadata the mod the config belongs to
     */
    public ConfigBuilder(ModMetadata metadata) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
    }

    /**
     * Scopes subsequently defined values into {@code name} (letters, digits, {@code _-} only).
     *
     * @param name the category name
     * @return this builder
     * @throws IllegalArgumentException for other characters
     */
    public ConfigBuilder category(String name) {
        Objects.requireNonNull(name, "name");
        if (!name.matches("[A-Za-z0-9_\\-]+")) {
            throw new IllegalArgumentException("Invalid category name: " + name);
        }
        this.category = name;
        return this;
    }

    /**
     * Returns to the root level.
     *
     * @return this builder
     */
    public ConfigBuilder root() {
        this.category = "";
        return this;
    }

    /**
     * Declares the schema version of this config; defaults to {@code 1}.
     *
     * @param version the current version, {@code 1} or more
     * @return this builder
     */
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
     *
     * @param fromVersion the old schema version this migration upgrades
     * @param migration   edits the raw JSON in place
     * @return this builder
     */
    public ConfigBuilder migrate(int fromVersion, Consumer<JsonObject> migration) {
        if (fromVersion < 1) {
            throw new IllegalArgumentException("fromVersion must be >= 1");
        }
        migrations.put(fromVersion, Objects.requireNonNull(migration, "migration"));
        return this;
    }

    /**
     * Adds an on/off value.
     *
     * @param key          the key, unique within the current category
     * @param defaultValue the value until the player changes it
     * @param description  what it does; shown in the config screen and the file
     * @return the live value; keep it in a constant and read it with {@code get()}
     */
    public ConfigValue<Boolean> booleanValue(String key, boolean defaultValue, String description) {
        return add(new ConfigValue.BooleanValue(category, key, defaultValue, description));
    }

    /**
     * Adds a whole number in {@code min..max}. Values out of range in the file are clamped.
     *
     * @param key          the key, unique within the current category
     * @param defaultValue the value until the player changes it
     * @param min          the lowest allowed value (inclusive)
     * @param max          the highest allowed value (inclusive)
     * @param description  what it does; shown in the config screen and the file
     * @return the live value; keep it in a constant and read it with {@code get()}
     */
    public ConfigValue<Integer> intValue(String key, int defaultValue, int min, int max, String description) {
        return add(new ConfigValue.IntegerValue(category, key, defaultValue, min, max, description));
    }

    /**
     * Adds a long in {@code min..max}. Values out of range in the file are clamped.
     *
     * @param key          the key, unique within the current category
     * @param defaultValue the value until the player changes it
     * @param min          the lowest allowed value (inclusive)
     * @param max          the highest allowed value (inclusive)
     * @param description  what it does; shown in the config screen and the file
     * @return the live value; keep it in a constant and read it with {@code get()}
     */
    public ConfigValue<Long> longValue(String key, long defaultValue, long min, long max, String description) {
        return add(new ConfigValue.LongValue(category, key, defaultValue, min, max, description));
    }

    /**
     * Adds a decimal number in {@code min..max}. Values out of range in the file are clamped.
     *
     * @param key          the key, unique within the current category
     * @param defaultValue the value until the player changes it
     * @param min          the lowest allowed value (inclusive)
     * @param max          the highest allowed value (inclusive)
     * @param description  what it does; shown in the config screen and the file
     * @return the live value; keep it in a constant and read it with {@code get()}
     */
    public ConfigValue<Double> doubleValue(String key, double defaultValue, double min, double max,
            String description) {
        return add(new ConfigValue.DoubleValue(category, key, defaultValue, min, max, description));
    }

    /**
     * Adds a text value of up to 512 characters.
     *
     * @param key          the key, unique within the current category
     * @param defaultValue the value until the player changes it
     * @param description  what it does; shown in the config screen and the file
     * @return the live value; keep it in a constant and read it with {@code get()}
     */
    public ConfigValue<String> stringValue(String key, String defaultValue, String description) {
        return stringValue(key, defaultValue, 512, description);
    }

    /**
     * Adds a text value of up to {@code maxLength} characters. Longer text in the file is cut.
     *
     * @param key          the key, unique within the current category
     * @param defaultValue the value until the player changes it
     * @param maxLength    the longest allowed text
     * @param description  what it does; shown in the config screen and the file
     * @return the live value; keep it in a constant and read it with {@code get()}
     */
    public ConfigValue<String> stringValue(String key, String defaultValue, int maxLength, String description) {
        return add(new ConfigValue.StringValue(category, key, defaultValue, maxLength, description));
    }

    /**
     * Adds a choice between the constants of an enum. Unknown names in the file fall back to the default.
     *
     * @param <E>          the enum type
     * @param key          the key, unique within the current category
     * @param type         the enum class
     * @param defaultValue the value until the player changes it
     * @param description  what it does; shown in the config screen and the file
     * @return the live value; keep it in a constant and read it with {@code get()}
     */
    public <E extends Enum<E>> ConfigValue<E> enumValue(String key, Class<E> type, E defaultValue,
            String description) {
        return add(new ConfigValue.EnumValue<>(category, key, type, defaultValue, description));
    }

    /**
     * Adds a list of strings (for example item or mod ids).
     *
     * @param key          the key, unique within the current category
     * @param defaultValue the value until the player changes it
     * @param description  what it does; shown in the config screen and the file
     * @return the live value; keep it in a constant and read it with {@code get()}
     */
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

    /**
     * Finalises the config: loads {@code config/<modid>.json} and returns the live handle.
     *
     * @return the live config, also registered with {@code Mycel.getConfig(modId)}
     * @throws IllegalStateException when called a second time
     */
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

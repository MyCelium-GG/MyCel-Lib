package my.celium.org.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.google.gson.JsonObject;

import my.celium.org.internal.JsonConfigStore;
import my.celium.org.logging.MycelLog;
import my.celium.org.metadata.ModMetadata;

/**
 * Live handle for one mod's configuration, backed by {@code config/<modid>.json}.
 *
 * <p>Obtained once via {@link ConfigBuilder#build()}. Values are read through
 * the {@link ConfigValue} instances returned by the builder; this handle adds
 * lifecycle operations: {@link #save()}, {@link #reload()}, {@link #resetAll()}
 * and category introspection for the config screen.
 */
public final class MycelConfig {
    private final ModMetadata metadata;
    private final List<ConfigValue<?>> values;
    private final int schemaVersion;
    private final Map<Integer, Consumer<JsonObject>> migrations;
    private final Path file;
    private volatile boolean dirty;

    MycelConfig(ModMetadata metadata, List<ConfigValue<?>> values, int schemaVersion,
            Map<Integer, Consumer<JsonObject>> migrations) {
        this.metadata = metadata;
        this.values = values;
        this.schemaVersion = schemaVersion;
        this.migrations = migrations;
        this.file = JsonConfigStore.fileFor(metadata.id());
        for (ConfigValue<?> value : values) {
            value.attach(this);
        }
    }

    public ModMetadata metadata() {
        return metadata;
    }

    public String modId() {
        return metadata.id();
    }

    /** All values in declaration order. */
    public List<ConfigValue<?>> values() {
        return Collections.unmodifiableList(values);
    }

    /** Distinct categories in first-use order (root {@code ""} first when present). */
    public List<String> categories() {
        List<String> result = new ArrayList<>();
        for (ConfigValue<?> value : values) {
            if (!result.contains(value.category())) {
                result.add(value.category());
            }
        }
        result.sort((a, b) -> a.isEmpty() ? -1 : b.isEmpty() ? 1 : a.compareTo(b));
        return Collections.unmodifiableList(result);
    }

    /** Values of one category in declaration order. */
    public List<ConfigValue<?>> valuesIn(String category) {
        List<ConfigValue<?>> result = new ArrayList<>();
        for (ConfigValue<?> value : values) {
            if (Objects.equals(value.category(), category)) {
                result.add(value);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Finds a value by path ({@code "key"} or {@code "category.key"}).
     *
     * @return the value, or {@code null} when nothing (or ambiguous bare key) matches
     * @see ConfigStrings#find(MycelConfig, String)
     */
    public ConfigValue<?> find(String path) {
        return ConfigStrings.find(this, path);
    }

    public Path file() {
        return file;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    /** Writes the file when anything changed since the last save/load. */
    public synchronized void save() {
        if (!dirty) {
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty(JsonConfigStore.VERSION_KEY, schemaVersion);
        Map<String, JsonObject> sections = new LinkedHashMap<>();
        for (ConfigValue<?> value : values) {
            JsonObject section = sections.computeIfAbsent(value.category(), c -> {
                JsonObject created = new JsonObject();
                if (!c.isEmpty()) {
                    root.add(c, created);
                }
                return created;
            });
            if (value.category().isEmpty()) {
                root.add(value.key(), value.saveRaw());
            } else {
                section.add(value.key(), value.saveRaw());
            }
        }
        JsonConfigStore.write(metadata.id(), file, root);
        dirty = false;
    }

    /** Re-reads the file; values that changed fire their listeners. */
    public synchronized void reload() {
        JsonObject root = JsonConfigStore.read(metadata.id(), file);
        applyRoot(root);
        dirty = false;
    }

    /** Resets every value to its default and saves. */
    public synchronized void resetAll() {
        for (ConfigValue<?> value : values) {
            value.reset();
        }
        save();
    }

    /** Returns {@code true} when every value holds its default. */
    public boolean isDefault() {
        for (ConfigValue<?> value : values) {
            if (!value.isDefault()) {
                return false;
            }
        }
        return true;
    }

    synchronized void load() {
        JsonObject root = JsonConfigStore.read(metadata.id(), file);
        applyRoot(root);
        // Persist immediately so new defaults/migrations land on disk.
        dirty = true;
        save();
    }

    private void applyRoot(JsonObject root) {
        int fileVersion = JsonConfigStore.versionOf(root);
        if (fileVersion > schemaVersion) {
            MycelLog.warnMod(metadata.id(),
                    "Config file is newer (v{}) than this version understands (v{}); keeping readable values",
                    fileVersion, schemaVersion);
        } else {
            for (int from = fileVersion; from < schemaVersion; from++) {
                Consumer<JsonObject> migration = migrations.get(from);
                if (migration != null) {
                    try {
                        migration.accept(root);
                    } catch (RuntimeException e) {
                        MycelLog.warnMod(metadata.id(), "Config migration v{} -> v{} failed: {}",
                                from, from + 1, e.toString());
                    }
                }
            }
        }
        for (ConfigValue<?> value : values) {
            JsonObject section = value.category().isEmpty() ? root : sectionOf(root, value.category());
            value.loadRaw(section == null ? null : section.get(value.key()));
        }
    }

    private static JsonObject sectionOf(JsonObject root, String category) {
        if (root.has(category) && root.get(category).isJsonObject()) {
            return root.getAsJsonObject(category);
        }
        return null;
    }

    void markDirty() {
        dirty = true;
        // Configs are tiny; save eagerly so crashes never lose settings.
        // IO failure must never break gameplay, so save() swallows into a warning.
        try {
            save();
        } catch (RuntimeException e) {
            MycelLog.warnMod(metadata.id(), "Failed to save config: {}", e.toString());
        }
    }
}

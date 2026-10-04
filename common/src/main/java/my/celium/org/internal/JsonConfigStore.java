package my.celium.org.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import my.celium.org.logging.MycelLog;

/**
 * Internal Gson-backed storage for {@code config/<modid>.json} files.
 *
 * <p>Not public API. Corrupt files are renamed to {@code .bak} (never deleted)
 * and replaced with defaults; writes are atomic (temp file + move) so a crash
 * mid-save cannot leave half-written JSON behind.
 */
public final class JsonConfigStore {
    public static final String VERSION_KEY = "mycel_config_version";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_BYTES = 1024 * 1024;

    private JsonConfigStore() {
    }

    /** Resolves (and creates the parent of) {@code config/<modId>.json}. */
    public static Path fileFor(String modId) {
        Path dir = PlatformHolder.get().configDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            MycelLog.warnMod(modId, "Could not create config dir {}: {}", dir, e.toString());
        }
        return dir.resolve(modId + ".json");
    }

    /** Reads the file; missing/corrupt files yield an empty object (version 1 assumed when absent). */
    public static JsonObject read(String modId, Path file) {
        if (!Files.isRegularFile(file)) {
            return new JsonObject();
        }
        try {
            long size = Files.size(file);
            if (size > MAX_BYTES) {
                MycelLog.warnMod(modId, "Config file suspiciously large ({} bytes); using defaults", size);
                return new JsonObject();
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException | IOException e) {
            Path backup = file.resolveSibling(file.getFileName() + ".bak");
            try {
                Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
                MycelLog.warnMod(modId, "Config file was corrupt; moved to {} and restored defaults", backup);
            } catch (IOException moveFailed) {
                MycelLog.warnMod(modId, "Config file was corrupt and could not be backed up: {}", e.toString());
            }
            return new JsonObject();
        }
    }

    /** Atomically writes the file. */
    public static void write(String modId, Path file, JsonObject root) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            MycelLog.warnMod(modId, "Could not write config file {}: {}", file, e.toString());
        }
    }

    /** Schema version stored in the file, or {@code 1} when absent/unreadable. */
    public static int versionOf(JsonObject root) {
        try {
            if (root.has(VERSION_KEY) && root.get(VERSION_KEY).isJsonPrimitive()) {
                return Math.max(1, root.get(VERSION_KEY).getAsInt());
            }
        } catch (RuntimeException ignored) {
            // Fall through to 1.
        }
        return 1;
    }
}

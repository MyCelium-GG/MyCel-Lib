package my.celium.org.data;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import my.celium.org.Mycel;
import my.celium.org.logging.MycelLog;
import my.celium.org.metadata.ModMetadata;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * One JSON file a mod keeps its own data in (pets, highlights, saved lists),
 * with the safety Mycel's config files have:
 * <ul>
 *   <li>writes are atomic (temp file + move), so a crash mid-save never
 *       leaves half-written JSON behind;</li>
 *   <li>a file that cannot be parsed is moved aside to
 *       {@code <name>.broken} (never deleted or overwritten) and reading
 *       continues with the fallback, so the next save cannot destroy it;</li>
 *   <li>files over the size cap (16&nbsp;MiB by default) are refused the same way.</li>
 * </ul>
 *
 * <pre>{@code
 * static final MycelDataFile FILE = MycelDataFile.inConfigDir(META, "mymod_data.json");
 *
 * Root root = FILE.read(Root.class, Root::new);   // never null, never throws
 * root.entries.put(key, value);
 * FILE.write(root);
 * }</pre>
 *
 * <p>Reading and writing are synchronous file IO; for big files call them from
 * {@code MycelScheduler.runAsync} and hop back with {@code runOnServer}.
 * Instances are immutable and thread-safe; callers own the locking of the
 * data they write.
 */
public final class MycelDataFile {
    /** Default size cap: larger files are treated as broken. */
    public static final int DEFAULT_MAX_BYTES = 16 * 1024 * 1024;

    private static final Gson DEFAULT_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final String modId;
    private final Path path;
    private final Gson gson;
    private final int maxBytes;

    private MycelDataFile(String modId, Path path, Gson gson, int maxBytes) {
        this.modId = modId;
        this.path = path;
        this.gson = gson;
        this.maxBytes = maxBytes;
    }

    /**
     * A file at an explicit path.
     *
     * @param mod  the owning mod, named in log lines
     * @param path where the file lives
     * @return the file handle; nothing is read or created yet
     */
    public static MycelDataFile at(ModMetadata mod, Path path) {
        Objects.requireNonNull(mod, "mod");
        Objects.requireNonNull(path, "path");
        return new MycelDataFile(mod.id(), path, DEFAULT_GSON, DEFAULT_MAX_BYTES);
    }

    /**
     * A file in the loader's {@code config} folder, e.g. {@code config/mymod_data.json}.
     *
     * @param mod      the owning mod, named in log lines
     * @param fileName a plain file name, no folders
     * @return the file handle; nothing is read or created yet
     */
    public static MycelDataFile inConfigDir(ModMetadata mod, String fileName) {
        return at(mod, Mycel.getConfigDir().resolve(checkFileName(fileName)));
    }

    /**
     * A file that belongs to the current world, at
     * {@code <world>/data/<modid>/<fileName>}. Use it for state that should
     * travel with the save, not with the player's game folder.
     *
     * @param server   the running server, whose world it belongs to
     * @param mod      the owning mod, named in log lines
     * @param fileName a plain file name, no folders
     * @return the file handle; nothing is read or created yet
     */
    public static MycelDataFile inWorld(MinecraftServer server, ModMetadata mod, String fileName) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(mod, "mod");
        Path dir = server.getWorldPath(LevelResource.DATA).resolve(mod.id());
        return at(mod, dir.resolve(checkFileName(fileName)));
    }

    /**
     * The same file, (de)serialized with {@code gson} (e.g. with type adapters).
     *
     * @param gson the Gson to use
     * @return a new handle
     */
    public MycelDataFile withGson(Gson gson) {
        return new MycelDataFile(modId, path, Objects.requireNonNull(gson, "gson"), maxBytes);
    }

    /**
     * The same file with another size cap in bytes.
     *
     * @param maxBytes the largest file still read, in bytes
     * @return a new handle
     */
    public MycelDataFile withMaxBytes(int maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        return new MycelDataFile(modId, path, gson, maxBytes);
    }

    /**
     * Where the file lives.
     *
     * @return the path
     */
    public Path path() {
        return path;
    }

    /**
     * Whether the file is on disk right now.
     *
     * @return {@code true} when it exists as a regular file
     */
    public boolean exists() {
        return Files.isRegularFile(path);
    }

    /**
     * Reads the file as a JSON object. Missing file: an empty object. Broken
     * file (unreadable, not JSON, not an object, too big): moved aside and an
     * empty object.
     *
     * @return the object; never {@code null}
     */
    public JsonObject readObject() {
        JsonElement element = readTree();
        if (element == null) {
            return new JsonObject();
        }
        if (!element.isJsonObject()) {
            quarantine("expected a JSON object");
            return new JsonObject();
        }
        return element.getAsJsonObject();
    }

    /**
     * Reads the file into {@code type} with this file's Gson. Missing or
     * broken files yield {@code fallback.get()}; a broken file is moved aside
     * first. Never returns {@code null} unless {@code fallback} does.
     *
     * @param <T>      the data type
     * @param type     the class to read into
     * @param fallback supplies the value for a missing or broken file
     * @return the data, or the fallback's value
     */
    public <T> T read(Class<T> type, Supplier<T> fallback) {
        return read((Type) type, fallback);
    }

    /**
     * Like {@link #read(Class, Supplier)} for generic types (use a Gson {@code TypeToken}).
     *
     * @param <T>      the data type
     * @param type     the type to read into
     * @param fallback supplies the value for a missing or broken file
     * @return the data, or the fallback's value
     */
    public <T> T read(Type type, Supplier<T> fallback) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(fallback, "fallback");
        JsonElement element = readTree();
        if (element == null) {
            return fallback.get();
        }
        try {
            T value = gson.fromJson(element, type);
            if (value != null) {
                return value;
            }
        } catch (JsonParseException | IllegalStateException | ClassCastException e) {
            quarantine(e.getMessage() == null ? e.toString() : e.getMessage());
            return fallback.get();
        }
        return fallback.get();
    }

    /**
     * Writes {@code value} (serialized with this file's Gson), atomically.
     *
     * @param value the data; a {@code JsonElement} is written as it is
     * @return whether it was saved (failures are logged, never thrown)
     */
    public boolean write(Object value) {
        Objects.requireNonNull(value, "value");
        JsonElement tree = value instanceof JsonElement element ? element : gson.toJsonTree(value);
        return writeText(gson.toJson(tree));
    }

    /**
     * Moves the file aside to {@code <name>.broken} (or a timestamped name
     * when that exists already) so the next write cannot overwrite it. Call
     * it when the data parsed but your own validation rejected it. Missing
     * files are ignored.
     *
     * @param reason why, for the log line
     * @return the path the file was moved to, or {@code null} when nothing moved
     */
    public Path quarantine(String reason) {
        if (!Files.exists(path)) {
            return null;
        }
        Path target = path.resolveSibling(path.getFileName() + ".broken");
        if (Files.exists(target)) {
            target = path.resolveSibling(path.getFileName() + ".broken-" + System.currentTimeMillis());
        }
        try {
            Files.move(path, target);
            MycelLog.warnMod(modId, "{} could not be read ({}); kept it as {} and started fresh",
                    path.getFileName(), reason, target.getFileName());
            return target;
        } catch (IOException e) {
            MycelLog.warnMod(modId, "{} could not be read ({}) nor moved aside: {}",
                    path.getFileName(), reason, e.toString());
            return null;
        }
    }

    /**
     * Deletes the file if present.
     *
     * @return whether it is gone (also {@code true} when it never existed)
     */
    public boolean delete() {
        try {
            Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            MycelLog.warnMod(modId, "Could not delete {}: {}", path, e.toString());
            return false;
        }
    }

    // Null means "use the fallback": missing, or broken and already moved aside.
    private JsonElement readTree() {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        String text;
        try {
            long size = Files.size(path);
            if (size > maxBytes) {
                quarantine("larger than " + maxBytes + " bytes");
                return null;
            }
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            quarantine("not valid UTF-8");
            return null;
        } catch (IOException e) {
            // Unreadable right now (permissions, locked): leave it alone, it may be fine.
            MycelLog.warnMod(modId, "Could not read {}: {}", path, e.toString());
            return null;
        }
        if (text.isBlank()) {
            return null;
        }
        try {
            return JsonParser.parseString(text);
        } catch (JsonParseException e) {
            quarantine("not valid JSON");
            return null;
        }
    }

    private boolean writeText(String text) {
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(tmp, text, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            MycelLog.warnMod(modId, "Could not save {}: {}", path, e.toString());
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // The temp file is harmless; the next write replaces it.
            }
            return false;
        }
    }

    private static String checkFileName(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.contains("/") || fileName.contains("\\")
                || fileName.equals(".") || fileName.equals("..")) {
            throw new IllegalArgumentException("Invalid data file name: " + fileName);
        }
        return fileName;
    }
}

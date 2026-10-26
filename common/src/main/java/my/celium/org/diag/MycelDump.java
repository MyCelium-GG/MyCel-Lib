package my.celium.org.diag;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import my.celium.org.Mycel;
import my.celium.org.config.ConfigStrings;
import my.celium.org.config.ConfigValue;
import my.celium.org.config.MycelConfig;
import my.celium.org.internal.MycelExecutors;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.platform.LoadedMod;
import my.celium.org.platform.PlatformService;
import my.celium.org.update.UpdateChecker;
import my.celium.org.update.UpdateResult;
import net.minecraft.server.MinecraftServer;

/**
 * {@code /mycel dump}: one JSON report with versions, every loaded mod, each
 * Mycel mod's config and the tail of {@code logs/latest.log}. It is always
 * saved to {@code mycel-dumps/} and, unless {@code allowDumpUploads} is off in
 * {@code mycel.json}, uploaded to pastes.dev and opened with the viewer at
 * {@link #VIEWER}.
 *
 * <p>IP addresses, access tokens and the OS home folder are removed before
 * anything leaves the machine. Pastes are public to anyone with the link.
 */
public final class MycelDump {
    /** Viewer base; the paste key is appended. */
    public static final String VIEWER = "https://dumps.mycelium-gg.org/";
    /** Bumped when the JSON shape changes incompatibly; the viewer reads it. */
    public static final int FORMAT_VERSION = 1;

    private static final URI UPLOAD = URI.create("https://api.pastes.dev/post");
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final int LOG_TAIL_BYTES = 512 * 1024;
    private static final int MAX_UPLOAD_BYTES = 2 * 1024 * 1024;
    private static final long COOLDOWN_MILLIS = 30_000;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .executor(MycelExecutors.async())
            .build();

    private static final Pattern IPV4 = Pattern.compile(
            // Loopback/any stay; "26.3.0.1-beta" and "v1.2.3.4" are versions, not addresses.
            "(?<![\\w.\\-])(?!127\\.0\\.0\\.1(?![\\d.]))(?!0\\.0\\.0\\.0(?![\\d.]))"
                    + "(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}(?![\\w.\\-+])");
    private static final Pattern IPV6 = Pattern.compile(
            "(?<![\\w:])(?:[0-9a-fA-F]{1,4}:){4,7}[0-9a-fA-F]{1,4}(?![\\w:])");
    private static final Pattern SECRET = Pattern.compile(
            "(?i)(--accessToken\\s+|access_?token[=:\"\\s]+|session id is |token:)[^\\s\"',]+");

    private static final AtomicLong lastUpload = new AtomicLong();
    private static volatile boolean uploadsEnabled = true;

    private MycelDump() {
    }

    /**
     * Called by {@code Mycel} from the global config; mods should not call this.
     *
     * @param enabled whether {@code /mycel dump} may upload
     */
    public static void setUploadsEnabled(boolean enabled) {
        uploadsEnabled = enabled;
    }

    /**
     * Whether uploads are allowed (the {@code dumpUploads} setting in Mycel's config).
     *
     * @return {@code true} when allowed
     */
    public static boolean uploadsEnabled() {
        return uploadsEnabled;
    }

    /**
     * Seconds left before another upload is allowed, {@code 0} when ready.
     *
     * @return the seconds to wait
     */
    public static long cooldownSeconds() {
        long left = lastUpload.get() + COOLDOWN_MILLIS - System.currentTimeMillis();
        return left <= 0 ? 0 : (left + 999) / 1000;
    }

    // ------------------------------------------------------------ collect

    /**
     * Builds the report. Call on the server thread: it reads live config values.
     *
     * @param server the running server
     * @return the report, already redacted
     */
    public static JsonObject collect(MinecraftServer server) {
        PlatformService platform = PlatformHolder.get();
        JsonObject root = new JsonObject();
        root.addProperty("format", "mycel-dump");
        root.addProperty("formatVersion", FORMAT_VERSION);
        root.addProperty("created", Instant.now().toString());
        root.addProperty("mycel", Mycel.getVersion());
        root.add("environment", environment(platform, server));
        root.add("mycelMods", mycelMods());
        root.add("mods", loadedMods(platform));
        JsonObject log = log(platform.gameDir().resolve("logs").resolve("latest.log"));
        if (log != null) {
            root.add("log", log);
        }
        return root;
    }

    private static JsonObject environment(PlatformService platform, MinecraftServer server) {
        Runtime runtime = Runtime.getRuntime();
        JsonObject env = new JsonObject();
        env.addProperty("loader", platform.platform().displayName());
        env.addProperty("loaderVersion", platform.loaderVersion());
        env.addProperty("minecraft", platform.minecraftVersion());
        env.addProperty("side", platform.isClient() ? "client" : "server");
        if (server != null) {
            env.addProperty("server", server.isDedicatedServer() ? "dedicated" : "integrated");
            env.addProperty("players", server.getPlayerCount());
        }
        env.addProperty("java", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        env.addProperty("os", System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + ")");
        env.addProperty("cpus", runtime.availableProcessors());
        env.addProperty("memoryMaxMb", runtime.maxMemory() / (1024 * 1024));
        env.addProperty("memoryUsedMb", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        env.addProperty("uptimeMinutes", ManagementFactory.getRuntimeMXBean().getUptime() / 60_000);
        env.addProperty("jvmArgs", redact(String.join(" ",
                ManagementFactory.getRuntimeMXBean().getInputArguments())));
        return env;
    }

    private static JsonArray mycelMods() {
        JsonArray out = new JsonArray();
        for (ModMetadata mod : Mycel.getRegisteredMods()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", mod.id());
            entry.addProperty("name", mod.name());
            entry.addProperty("version", mod.version());
            entry.addProperty("enabled", Mycel.isEnabled(mod.id()));
            UpdateResult update = UpdateChecker.lastResult(mod.id());
            if (update != null) {
                entry.addProperty("latest", update.latestVersion());
            }
            MycelConfig config = Mycel.getConfig(mod.id());
            if (config != null) {
                JsonArray values = new JsonArray();
                for (ConfigValue<?> value : config.values()) {
                    JsonObject row = new JsonObject();
                    row.addProperty("path", ConfigStrings.pathOf(value));
                    row.addProperty("value", redact(ConfigStrings.valueToString(value)));
                    row.addProperty("default", ConfigStrings.defaultToString(value));
                    row.addProperty("changed", !value.isDefault());
                    row.addProperty("type", ConfigStrings.describe(value));
                    values.add(row);
                }
                entry.add("config", values);
            }
            out.add(entry);
        }
        return out;
    }

    private static JsonArray loadedMods(PlatformService platform) {
        List<LoadedMod> mods = new ArrayList<>(platform.loadedMods());
        mods.sort(Comparator.comparing(LoadedMod::id));
        JsonArray out = new JsonArray();
        for (LoadedMod mod : mods) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", mod.id());
            entry.addProperty("name", mod.name());
            entry.addProperty("version", mod.version());
            out.add(entry);
        }
        return out;
    }

    private static JsonObject log(Path file) {
        try {
            if (!Files.isRegularFile(file)) {
                return null;
            }
            String tail = tail(file, LOG_TAIL_BYTES);
            JsonObject log = new JsonObject();
            log.addProperty("file", "logs/latest.log");
            log.addProperty("truncated", Files.size(file) > LOG_TAIL_BYTES);
            log.addProperty("text", redact(tail));
            return log;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** The last {@code maxBytes} of a file, starting at a whole line. */
    static String tail(Path file, int maxBytes) throws IOException {
        try (RandomAccessFile in = new RandomAccessFile(file.toFile(), "r")) {
            long size = in.length();
            long start = Math.max(0, size - maxBytes);
            byte[] bytes = new byte[(int) (size - start)];
            in.seek(start);
            in.readFully(bytes);
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (start > 0) {
                int newline = text.indexOf('\n');
                text = newline < 0 ? "" : text.substring(newline + 1);
            }
            return text;
        }
    }

    /**
     * Removes IP addresses, tokens and the OS home folder (which names the OS user).
     *
     * @param text any text
     * @return a copy with those parts replaced by placeholders like {@code <ip>}
     */
    public static String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String out = text;
        String home = System.getProperty("user.home");
        if (home != null && home.length() > 3) {
            out = out.replace(home, "~");
            out = out.replace(home.replace('\\', '/'), "~");
        }
        out = SECRET.matcher(out).replaceAll("$1<redacted>");
        out = IPV4.matcher(out).replaceAll("<ip>");
        out = IPV6.matcher(out).replaceAll("<ip>");
        return out;
    }

    // ------------------------------------------------------------ save + upload

    /**
     * Pretty JSON text of a report.
     *
     * @param dump the report from {@link #collect(MinecraftServer)}
     * @return the JSON text
     */
    public static String toJson(JsonObject dump) {
        return GSON.toJson(dump);
    }

    /**
     * Writes the report to {@code <game dir>/mycel-dumps/dump-<time>.json} and returns the path.
     *
     * @param json the report, from {@link #toJson(JsonObject)}
     * @return the file written
     * @throws IOException when the file cannot be written
     */
    public static Path save(String json) throws IOException {
        Path dir = PlatformHolder.get().gameDir().resolve("mycel-dumps");
        Files.createDirectories(dir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path file = dir.resolve("dump-" + stamp + ".json");
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return file;
    }

    /**
     * Uploads to pastes.dev off the game thread. Completes with the paste key,
     * or exceptionally with a short, player-readable message.
     *
     * @param json the report, from {@link #toJson(JsonObject)}
     * @return completes with the paste key; open it with {@link #viewerUrl(String)}
     */
    public static CompletableFuture<String> upload(String json) {
        if (!uploadsEnabled) {
            return CompletableFuture.failedFuture(new IllegalStateException("uploads are off in mycel.json"));
        }
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        if (body.length > MAX_UPLOAD_BYTES) {
            return CompletableFuture.failedFuture(new IllegalStateException("dump is too large to upload"));
        }
        lastUpload.set(System.currentTimeMillis());
        HttpRequest request = HttpRequest.newBuilder(UPLOAD)
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mycel/" + Mycel.getVersion() + " (+https://wiki.mycelium-gg.org)")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException("pastes.dev answered HTTP " + response.statusCode());
                    }
                    String key = keyOf(response.body(), response.headers().firstValue("Location").orElse(null));
                    if (key == null) {
                        throw new IllegalStateException("pastes.dev sent no paste key");
                    }
                    return key;
                });
    }

    /** Paste key from the JSON body ({@code {"key": "..."}}), falling back to the Location header. */
    static String keyOf(String body, String location) {
        String key = null;
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("key")) {
                key = json.get("key").getAsString();
            }
        } catch (RuntimeException e) {
            // fall back to the header
        }
        if (key == null && location != null) {
            key = location.substring(location.lastIndexOf('/') + 1);
        }
        return key != null && key.matches("[A-Za-z0-9]{4,64}") ? key : null;
    }

    /**
     * Viewer link for a paste key.
     *
     * @param key the paste key from {@link #upload(String)}
     * @return the link to share
     */
    public static String viewerUrl(String key) {
        return VIEWER + key;
    }
}

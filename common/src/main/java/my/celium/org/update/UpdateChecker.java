package my.celium.org.update;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import my.celium.org.internal.MycelExecutors;
import my.celium.org.logging.MycelLog;
import my.celium.org.metadata.ModMetadata;

/**
 * Optional asynchronous update checker.
 *
 * <p>Guarantees:
 * <ul>
 *   <li>never blocks the game thread or startup (everything runs on Mycel's
 *       shared background pool);</li>
 *   <li>strict timeouts (8s connect, 12s total) and a 256&nbsp;KiB response cap;</li>
 *   <li>HTTPS only - any other scheme is refused;</li>
 *   <li>results cached per mod (re-checked at most once per 6 hours);</li>
 *   <li>at most one chatty line per mod with an update; everything else is debug;</li>
 *   <li>any failure (DNS, TLS, HTTP error, malformed JSON) degrades to silence.</li>
 * </ul>
 *
 * <p>Expected document shape (extra fields ignored):
 * <pre>{@code {"version": "1.2.0", "url": "https://example.com/mymod"}}</pre>
 *
 * <p>A mod opts in by setting {@code updateUrl} in its {@link ModMetadata} and
 * opts out by leaving it {@code null}. The global {@code mycel.json} flag
 * {@code enableUpdateChecker} disables all checks. No update service is
 * hardcoded - any HTTPS endpoint serving the shape above works.
 */
public final class UpdateChecker {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(12);
    private static final int MAX_BYTES = 256 * 1024;
    private static final long CACHE_MILLIS = 6L * 60 * 60 * 1000;
    private static final Gson GSON = new Gson();

    private static final Map<String, UpdateResult> CACHE = new ConcurrentHashMap<>();
    private static volatile boolean globallyEnabled = true;

    private UpdateChecker() {
    }

    /** Called by {@code Mycel} from the global config; mods should not call this. */
    public static void setGloballyEnabled(boolean enabled) {
        globallyEnabled = enabled;
    }

    /**
     * Queues an update check for {@code mod}. No-op when globally disabled,
     * when the mod has no {@code updateUrl}, or when a fresh cached result exists.
     */
    public static void check(ModMetadata mod) {
        Objects.requireNonNull(mod, "mod");
        if (!globallyEnabled || mod.updateUrl() == null) {
            return;
        }
        UpdateResult cached = CACHE.get(mod.id());
        if (cached != null && System.currentTimeMillis() - cached.checkedAtMillis() < CACHE_MILLIS) {
            announceIfNeeded(cached);
            return;
        }
        MycelExecutors.async().execute(() -> {
            try {
                UpdateResult result = query(mod);
                CACHE.put(mod.id(), result);
                announceIfNeeded(result);
            } catch (Exception e) {
                MycelLog.debug("Update check for '{}' failed quietly: {}", mod.id(), e.toString());
            }
        });
    }

    /** Last result for a mod, or {@code null} when never checked. */
    public static UpdateResult lastResult(String modId) {
        return CACHE.get(modId);
    }

    private static void announceIfNeeded(UpdateResult result) {
        if (result.updateAvailable()) {
            String target = result.url() == null
                    ? result.latestVersion()
                    : result.latestVersion() + " (" + result.url() + ")";
            MycelLog.infoMod(result.modId(), "Update available: {} -> {}. See {}",
                    result.currentVersion(), target, result.url() == null ? "the project page" : result.url());
        }
    }

    private static UpdateResult query(ModMetadata mod) throws Exception {
        URI uri = URI.create(mod.updateUrl());
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("Refusing non-HTTPS update URL: " + mod.updateUrl());
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "Mycel/" + my.celium.org.Mycel.getVersion() + " (update-check)")
                .GET()
                .build();
        HttpResponse<byte[]> response =
                client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        byte[] body = response.body();
        if (body == null || body.length == 0 || body.length > MAX_BYTES) {
            throw new IllegalStateException("Bad response size: " + (body == null ? -1 : body.length));
        }
        JsonObject json = JsonParser
                .parseString(new String(body, StandardCharsets.UTF_8))
                .getAsJsonObject();
        if (!json.has("version") || !json.get("version").isJsonPrimitive()) {
            throw new IllegalStateException("Update document has no 'version' string");
        }
        String latest = json.get("version").getAsString().trim();
        String url = json.has("url") && json.get("url").isJsonPrimitive()
                ? json.get("url").getAsString().trim()
                : null;
        if (latest.isEmpty() || latest.length() > 64) {
            throw new IllegalStateException("Implausible version string");
        }
        boolean newer = compareVersions(latest, mod.version()) > 0;
        return new UpdateResult(mod.id(), mod.version(), latest,
                url == null || url.isEmpty() ? null : url, newer, System.currentTimeMillis());
    }

    /**
     * Compares dot/dash-separated versions. Numeric segments compare
     * numerically, others lexicographically; a version with more segments wins
     * only when all shared segments are equal and the remainder is numeric.
     * Returns negative/zero/positive like {@link Comparable#compareTo}.
     */
    static int compareVersions(String a, String b) {
        String[] left = a.split("[.\\-+]");
        String[] right = b.split("[.\\-+]");
        int length = Math.max(left.length, right.length);
        for (int i = 0; i < length; i++) {
            String l = i < left.length ? left[i] : "0";
            String r = i < right.length ? right[i] : "0";
            int compared = compareSegment(l, r);
            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    }

    private static int compareSegment(String l, String r) {
        Integer ln = tryParse(l);
        Integer rn = tryParse(r);
        if (ln != null && rn != null) {
            return Integer.compare(ln, rn);
        }
        if (ln != null) {
            return 1; // numeric beats qualifier ("1.2" > "1.2-beta")
        }
        if (rn != null) {
            return -1;
        }
        return l.compareToIgnoreCase(r);
    }

    private static Integer tryParse(String segment) {
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

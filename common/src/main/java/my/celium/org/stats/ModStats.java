package my.celium.org.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import my.celium.org.internal.stats.MycelStats;
import my.celium.org.metadata.ModMetadata;

/**
 * Anonymous usage stats for a mod, sent to the mod's own FastStats project through the FastStats
 * copy inside Mycel. The mod needs no FastStats dependency, works on every Minecraft line Mycel
 * supports, and follows the same consent as Mycel itself: players answer once on Mycel's
 * consent screen, dedicated servers can turn it off with {@code shareStats} in
 * {@code config/mycel.json}. Nothing is sent before that answer, or when stats are off.
 *
 * <pre>{@code
 * public static final ModStats STATS = ModStats.builder(META, "your-project-token")
 *         .number("pets_tracked", PetStore::count)
 *         .numberMap("species", PetStore::countBySpecies)
 *         .errors("com.example.mymod.")
 *         .start();
 *
 * STATS.count("command_find");  // usage counters, sent as the "usage" metric and reset after each send
 * }</pre>
 *
 * <p>Every metric id must exist as a data source in the FastStats project, or FastStats drops it.
 * Suppliers run on a background thread every 30 minutes; return {@code null} to skip a metric for
 * that send. A supplier that throws only skips that metric.
 *
 * @since 1.6.0
 */
public final class ModStats {
    private final ModMetadata mod;
    private final String token;
    private final List<Entry> metrics;
    private final String errorPackage;
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    /** Named counter maps from {@link Builder#counters(String)}: id -> key -> count. */
    private final Map<String, Map<String, AtomicInteger>> named;
    private volatile boolean running;

    private ModStats(Builder builder) {
        this.mod = builder.mod;
        this.token = builder.token;
        this.metrics = List.copyOf(builder.metrics);
        this.errorPackage = builder.errorPackage;
        Map<String, Map<String, AtomicInteger>> maps = new java.util.LinkedHashMap<>();
        for (String id : builder.counterIds) {
            maps.put(id, new ConcurrentHashMap<>());
        }
        this.named = java.util.Collections.unmodifiableMap(maps);
    }

    /**
     * Starts a stats setup for {@code mod}.
     *
     * @param mod   the mod; its id names the project in logs, its version is sent as the mod version
     * @param token the project token from faststats.dev (public, it ships in the jar)
     * @return a builder
     */
    public static Builder builder(ModMetadata mod, String token) {
        return new Builder(mod, token);
    }

    /**
     * Counts one use of something, like {@code command_find}. Sent as the {@code usage} metric
     * (a map of counts) and reset after each send. Does nothing while stats are off.
     *
     * @param what lowercase letters, digits and underscores
     */
    public void count(String what) {
        if (running && what != null) {
            counters.computeIfAbsent(what, ignored -> new AtomicInteger()).incrementAndGet();
        }
    }

    /**
     * Counts one {@code key} in the counter map {@code id} registered with
     * {@link Builder#counters(String)}, like {@code count("planned_items", "diamond_sword")}.
     * Sent as a map of counts and reset after each send. Does nothing while stats are off or for an
     * unknown id.
     *
     * @param id  the counter map's data source id
     * @param key what was counted, kept short (at most 64 characters)
     */
    public void count(String id, String key) {
        Map<String, AtomicInteger> map = named.get(id);
        if (running && map != null && key != null && !key.isEmpty() && key.length() <= 64
                && (map.size() < 200 || map.containsKey(key))) {
            map.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
        }
    }

    /**
     * Sends {@code error} when the player agreed to error reports and the error went through the
     * package given to {@link Builder#errors(String)}. Errors logged with
     * {@code MycelLog.errorMod(modId, ...)} are sent automatically.
     *
     * @param error the error
     */
    public void report(Throwable error) {
        if (running && error != null) {
            MycelStats.reportFor(this, error);
        }
    }

    /**
     * Whether this mod is sending stats right now.
     *
     * @return {@code true} once Mycel started it
     */
    public boolean isRunning() {
        return running;
    }

    // ------------------------------------------------------------ for Mycel's stats runtime

    /**
     * The mod these stats belong to.
     *
     * @return the mod
     * @hidden
     */
    public ModMetadata mod() {
        return mod;
    }

    /**
     * The FastStats project token.
     *
     * @return the token
     * @hidden
     */
    public String token() {
        return token;
    }

    /**
     * The registered metrics.
     *
     * @return the metrics
     * @hidden
     */
    public List<Entry> metrics() {
        return metrics;
    }

    /**
     * The package whose errors go to this project.
     *
     * @return the package prefix, or {@code null} when error reports are not wanted
     * @hidden
     */
    public String errorPackage() {
        return errorPackage;
    }

    /**
     * The usage counts since the last send.
     *
     * @return the counts, or {@code null} when nothing was counted
     * @hidden
     */
    public Map<String, Integer> counters() {
        return snapshot(counters);
    }

    /**
     * The ids registered with {@link Builder#counters(String)}.
     *
     * @return the ids
     * @hidden
     */
    public java.util.Set<String> counterIds() {
        return named.keySet();
    }

    /**
     * The counts of one counter map since the last send.
     *
     * @param id the counter map's id
     * @return the counts, or {@code null} when nothing was counted
     * @hidden
     */
    public Map<String, Integer> counters(String id) {
        Map<String, AtomicInteger> map = named.get(id);
        return map == null ? null : snapshot(map);
    }

    private static Map<String, Integer> snapshot(Map<String, AtomicInteger> source) {
        Map<String, Integer> out = new java.util.TreeMap<>();
        source.forEach((key, value) -> {
            int count = value.get();
            if (count > 0) {
                out.put(key, count);
            }
        });
        return out.isEmpty() ? null : out;
    }

    /**
     * Clears the usage counts after a send.
     *
     * @hidden
     */
    public void resetCounters() {
        counters.clear();
        named.values().forEach(Map::clear);
    }

    /**
     * Marks these stats as sending.
     *
     * @hidden
     */
    public void markRunning() {
        running = true;
    }

    /**
     * Kinds of metric values.
     *
     * @hidden
     */
    public enum Kind {
        /** A number. */
        NUMBER,
        /** A string. */
        STRING,
        /** A boolean. */
        BOOL,
        /** A list of strings. */
        STRING_ARRAY,
        /** A map of string to number. */
        NUMBER_MAP,
        /** A map of string to string. */
        STRING_MAP
    }

    /**
     * One metric: id, kind and where its value comes from.
     *
     * @param id       the data source id
     * @param kind     the value kind
     * @param supplier the value, read on each send
     * @hidden
     */
    public record Entry(String id, Kind kind, Supplier<?> supplier) {
    }

    /** Collects the metrics; {@link #start()} hands them to Mycel. */
    public static final class Builder {
        private final ModMetadata mod;
        private final String token;
        private final List<Entry> metrics = new ArrayList<>();
        private final List<String> counterIds = new ArrayList<>();
        private String errorPackage;

        private Builder(ModMetadata mod, String token) {
            this.mod = Objects.requireNonNull(mod, "mod");
            this.token = Objects.requireNonNull(token, "token");
        }

        private Builder add(String id, Kind kind, Supplier<?> supplier) {
            Objects.requireNonNull(supplier, "supplier");
            if (id == null || !id.matches("[a-z][a-z0-9_]{0,63}")) {
                throw new IllegalArgumentException("Metric id must be lowercase letters, digits and underscores: " + id);
            }
            if (id.equals("usage") || counterIds.contains(id) || metrics.stream().anyMatch(entry -> entry.id().equals(id))) {
                throw new IllegalArgumentException("Metric id already used: " + id);
            }
            metrics.add(new Entry(id, kind, supplier));
            return this;
        }

        /**
         * A number, like how many pets are tracked.
         *
         * @param id       the data source id
         * @param supplier the value
         * @return this builder
         */
        public Builder number(String id, Supplier<? extends Number> supplier) {
            return add(id, Kind.NUMBER, supplier);
        }

        /**
         * A string, like the chosen mode.
         *
         * @param id       the data source id
         * @param supplier the value
         * @return this builder
         */
        public Builder string(String id, Supplier<String> supplier) {
            return add(id, Kind.STRING, supplier);
        }

        /**
         * A yes/no value, like whether a setting is on.
         *
         * @param id       the data source id
         * @param supplier the value
         * @return this builder
         */
        public Builder bool(String id, Supplier<Boolean> supplier) {
            return add(id, Kind.BOOL, supplier);
        }

        /**
         * A list of strings, like the settings that differ from the defaults.
         *
         * @param id       the data source id
         * @param supplier the value
         * @return this builder
         */
        public Builder stringArray(String id, Supplier<String[]> supplier) {
            return add(id, Kind.STRING_ARRAY, supplier);
        }

        /**
         * Counts per name, like pets per species.
         *
         * @param id       the data source id
         * @param supplier the value
         * @return this builder
         */
        public Builder numberMap(String id, Supplier<? extends Map<String, ? extends Number>> supplier) {
            return add(id, Kind.NUMBER_MAP, supplier);
        }

        /**
         * Strings per name.
         *
         * @param id       the data source id
         * @param supplier the value
         * @return this builder
         */
        public Builder stringMap(String id, Supplier<? extends Map<String, String>> supplier) {
            return add(id, Kind.STRING_MAP, supplier);
        }

        /**
         * A map of counts filled with {@link ModStats#count(String, String)}, like which items were
         * planned. Sent as a number map and reset after each send, like {@code usage}.
         *
         * @param id the data source id
         * @return this builder
         */
        public Builder counters(String id) {
            if (id == null || !id.matches("[a-z][a-z0-9_]{0,63}")) {
                throw new IllegalArgumentException("Metric id must be lowercase letters, digits and underscores: " + id);
            }
            if (id.equals("usage") || counterIds.contains(id) || metrics.stream().anyMatch(entry -> entry.id().equals(id))) {
                throw new IllegalArgumentException("Metric id already used: " + id);
            }
            counterIds.add(id);
            return this;
        }

        /**
         * Also send error reports (if the player agreed) for errors with a stack frame in
         * {@code packagePrefix}, like {@code "com.example.mymod."}. User folder names, UUIDs and
         * IP addresses are removed. These errors then go to this project instead of Mycel's.
         *
         * @param packagePrefix the mod's package, ending with a dot
         * @return this builder
         */
        public Builder errors(String packagePrefix) {
            this.errorPackage = Objects.requireNonNull(packagePrefix, "packagePrefix");
            return this;
        }

        /**
         * Registers the stats with Mycel. They start once stats are allowed (right away when the
         * player already agreed), never before. Safe to call during mod init, on any side.
         *
         * @return the handle for counters and error reports
         */
        public ModStats start() {
            ModStats stats = new ModStats(this);
            MycelStats.register(stats);
            return stats;
        }
    }
}

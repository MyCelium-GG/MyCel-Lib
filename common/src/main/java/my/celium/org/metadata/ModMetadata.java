package my.celium.org.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Small immutable description of a mod that uses Mycel.
 *
 * <p>A dependent mod registers one instance via {@code Mycel.registerMod(...)}.
 * Mycel uses it for logging context, configuration file names, translation
 * namespaces, diagnostics and (opt-in) update checking.
 *
 * <p>Mycel never hardcodes known mods; any mod may register itself.
 */
public final class ModMetadata {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_\\-]+");

    private final String id;
    private final String name;
    private final String version;
    private final String website;
    private final List<String> authors;
    private final String updateUrl;

    private ModMetadata(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.version = builder.version;
        this.website = builder.website;
        this.authors = Collections.unmodifiableList(new ArrayList<>(builder.authors));
        this.updateUrl = builder.updateUrl;
    }

    /** Lowercase namespaced id, e.g. {@code "doubledoors"}. */
    public String id() {
        return id;
    }

    /** Human readable name, e.g. {@code "Double Doors"}. */
    public String name() {
        return name;
    }

    /** Mod version string, e.g. {@code "1.4.0"}. */
    public String version() {
        return version;
    }

    /** Project website, may be {@code null}. */
    public String website() {
        return website;
    }

    /** Authors, never {@code null} (possibly empty). */
    public List<String> authors() {
        return authors;
    }

    /**
     * Optional URL returning update metadata JSON for this mod.
     * Update checking is asynchronous, cached and failure-tolerant.
     * {@code null} disables update checking for this mod.
     */
    public String updateUrl() {
        return updateUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModMetadata other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name + " (" + id + " " + version + ")";
    }

    public static Builder builder(String id, String name, String version) {
        return new Builder(id, name, version);
    }

    /** Fluent builder with validation. */
    public static final class Builder {
        private final String id;
        private final String name;
        private final String version;
        private String website;
        private final List<String> authors = new ArrayList<>();
        private String updateUrl;

        private Builder(String id, String name, String version) {
            if (id == null || !ID_PATTERN.matcher(id).matches()) {
                throw new IllegalArgumentException(
                        "Mod id must match [a-z0-9_-]+, got: " + id);
            }
            this.id = id;
            this.name = Objects.requireNonNull(name, "name");
            this.version = Objects.requireNonNull(version, "version");
        }

        public Builder website(String website) {
            this.website = website;
            return this;
        }

        public Builder author(String author) {
            this.authors.add(Objects.requireNonNull(author, "author"));
            return this;
        }

        public Builder authors(List<String> authors) {
            this.authors.addAll(Objects.requireNonNull(authors, "authors"));
            return this;
        }

        /**
         * Opt in to update checking. Must be an {@code https://} URL that
         * serves a small JSON document. See {@code UpdateChecker} for the
         * expected shape and the safety rules (timeouts, size limits).
         */
        public Builder updateUrl(String updateUrl) {
            if (updateUrl != null && !updateUrl.startsWith("https://")) {
                throw new IllegalArgumentException("updateUrl must use https://, got: " + updateUrl);
            }
            this.updateUrl = updateUrl;
            return this;
        }

        public ModMetadata build() {
            return new ModMetadata(this);
        }
    }
}

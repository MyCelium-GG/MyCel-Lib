package my.celium.org.update;

/**
 * Outcome of one update check. Either produced from a remote document or a
 * cached value; {@link #updateAvailable()} is only {@code true} when the
 * remote version provably compares newer than the installed one.
 */
public final class UpdateResult {
    private final String modId;
    private final String currentVersion;
    private final String latestVersion;
    private final String url;
    private final boolean updateAvailable;
    private final long checkedAtMillis;

    public UpdateResult(String modId, String currentVersion, String latestVersion, String url,
            boolean updateAvailable, long checkedAtMillis) {
        this.modId = modId;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.url = url;
        this.updateAvailable = updateAvailable;
        this.checkedAtMillis = checkedAtMillis;
    }

    public String modId() {
        return modId;
    }

    public String currentVersion() {
        return currentVersion;
    }

    public String latestVersion() {
        return latestVersion;
    }

    /** Download/project page, may be {@code null}. */
    public String url() {
        return url;
    }

    public boolean updateAvailable() {
        return updateAvailable;
    }

    public long checkedAtMillis() {
        return checkedAtMillis;
    }
}

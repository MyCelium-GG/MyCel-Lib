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

    /**
     * Created by {@link UpdateChecker}; mods only read it.
     *
     * @param modId           the checked mod
     * @param currentVersion  the installed version
     * @param latestVersion   the newest version the document named
     * @param url             the download page, or {@code null}
     * @param updateAvailable whether {@code latestVersion} is newer
     * @param checkedAtMillis when the check ran ({@link System#currentTimeMillis()})
     */
    public UpdateResult(String modId, String currentVersion, String latestVersion, String url,
            boolean updateAvailable, long checkedAtMillis) {
        this.modId = modId;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.url = url;
        this.updateAvailable = updateAvailable;
        this.checkedAtMillis = checkedAtMillis;
    }

    /**
     * The checked mod.
     *
     * @return the mod id
     */
    public String modId() {
        return modId;
    }

    /**
     * The installed version.
     *
     * @return the version
     */
    public String currentVersion() {
        return currentVersion;
    }

    /**
     * The newest version the update document named.
     *
     * @return the version
     */
    public String latestVersion() {
        return latestVersion;
    }

    /**
     * Download/project page, may be {@code null}.
     *
     * @return the URL, or {@code null}
     */
    public String url() {
        return url;
    }

    /**
     * Whether the newest version is provably newer than the installed one.
     *
     * @return {@code true} when an update is available
     */
    public boolean updateAvailable() {
        return updateAvailable;
    }

    /**
     * When the check ran.
     *
     * @return the time, as {@link System#currentTimeMillis()}
     */
    public long checkedAtMillis() {
        return checkedAtMillis;
    }
}

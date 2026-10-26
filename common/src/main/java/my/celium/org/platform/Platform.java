package my.celium.org.platform;

/** The mod loader Mycel is currently running on. */
public enum Platform {
    /** Fabric Loader (with Fabric API). */
    FABRIC,
    /** Minecraft Forge. */
    FORGE,
    /** NeoForge. */
    NEOFORGE,
    /** Only used in unit tests / unknown environments. */
    UNKNOWN;

    /**
     * Lowercase name used in log lines and diagnostics.
     *
     * @return e.g. {@code "neoforge"}
     */
    public String displayName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}

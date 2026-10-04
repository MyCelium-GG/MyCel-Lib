package my.celium.org.platform;

/** The mod loader Mycel is currently running on. */
public enum Platform {
    FABRIC,
    FORGE,
    NEOFORGE,
    /** Only used in unit tests / unknown environments. */
    UNKNOWN;

    /** Lowercase name used in log lines and diagnostics. */
    public String displayName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}

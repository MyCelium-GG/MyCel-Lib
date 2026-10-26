package my.celium.org.config;

/** The kind of a {@link ConfigValue}; used by persistence and the config screen. */
public enum ValueKind {
    /** {@code true} or {@code false}. */
    BOOLEAN,
    /** An {@code int} with a range. */
    INTEGER,
    /** A {@code long} with a range. */
    LONG,
    /** A {@code double} with a range. */
    DOUBLE,
    /** Text with a maximum length. */
    STRING,
    /** One constant of an enum. */
    ENUM,
    /** A list of strings. */
    STRING_LIST
}

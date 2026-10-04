package my.celium.org.config;

/** The kind of a {@link ConfigValue}; used by persistence and the config screen. */
public enum ValueKind {
    BOOLEAN,
    INTEGER,
    LONG,
    DOUBLE,
    STRING,
    ENUM,
    STRING_LIST
}

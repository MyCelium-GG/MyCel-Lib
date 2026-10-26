package my.celium.org.platform;

/**
 * One mod as the loader sees it: id, display name and version.
 *
 * @param id      the mod id, e.g. {@code "betterpets"}
 * @param name    the display name
 * @param version the version string the mod declares
 */
public record LoadedMod(String id, String name, String version) {
}

package my.celium.org.platform;

/** One mod as the loader sees it: id, display name and version. */
public record LoadedMod(String id, String name, String version) {
}

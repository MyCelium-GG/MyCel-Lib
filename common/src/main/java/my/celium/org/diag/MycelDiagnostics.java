package my.celium.org.diag;

import java.util.ArrayList;
import java.util.List;

import my.celium.org.Mycel;
import my.celium.org.config.MycelConfig;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.platform.PlatformService;

/**
 * Lightweight diagnostics: versions, platform, registered mods, config state.
 * Surfaced through {@code /mycel info} and {@code /mycel mods}; noiseless
 * otherwise (nothing is logged unless asked).
 */
public final class MycelDiagnostics {
    private MycelDiagnostics() {
    }

    /** One short line per fact; stable order for tests and admins. */
    public static List<String> summaryLines() {
        PlatformService platform = PlatformHolder.get();
        List<String> lines = new ArrayList<>();
        lines.add("Mycel " + Mycel.getVersion() + " on " + platform.platform().displayName()
                + " (loader " + platform.loaderVersion() + ", MC " + platform.minecraftVersion() + ")");
        lines.add("Registered mods (" + Mycel.getRegisteredMods().size() + "):");
        for (ModMetadata mod : Mycel.getRegisteredMods()) {
            MycelConfig config = Mycel.getConfig(mod.id());
            String configState = config == null ? "no config" : config.values().size() + " values";
            lines.add(" - " + mod.id() + " " + mod.version() + " [" + configState + "]"
                    + (Mycel.isEnabled(mod.id()) ? "" : " [disabled]"));
        }
        return List.copyOf(lines);
    }
}

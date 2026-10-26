package my.celium.org.diag;

import java.util.ArrayList;
import java.util.List;

import my.celium.org.Mycel;
import my.celium.org.config.ConfigStrings;
import my.celium.org.config.ConfigValue;
import my.celium.org.config.MycelConfig;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.platform.PlatformService;
import my.celium.org.update.UpdateChecker;
import my.celium.org.update.UpdateResult;
import my.celium.org.util.TextUtil;
import net.minecraft.network.chat.MutableComponent;

/**
 * Lightweight diagnostics: versions, platform, registered mods, config state.
 * Surfaced through {@code /mycel info} and {@code /mycel mods}; noiseless
 * otherwise (nothing is logged unless asked).
 */
public final class MycelDiagnostics {
    private MycelDiagnostics() {
    }

    /**
     * One short line per fact; stable order for tests and admins.
     *
     * @return an unmodifiable list of plain lines
     */
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

    /**
     * Styled header for {@code /mycel info}: brand + platform + versions.
     *
     * @return the header line
     */
    public static MutableComponent headerLine() {
        PlatformService platform = PlatformHolder.get();
        return TextUtil.header("Mycel " + Mycel.getVersion(),
                platform.platform().displayName()
                        + " \u00B7 loader " + platform.loaderVersion()
                        + " \u00B7 MC " + platform.minecraftVersion());
    }

    /**
     * Styled row for a single registered mod, for use under a header.
     *
     * @param mod the mod
     * @return the row: name, version, state and any available update
     */
    public static MutableComponent modLine(ModMetadata mod) {
        MycelConfig config = Mycel.getConfig(mod.id());
        String configState = config == null ? "no config" : config.values().size() + " values";
        MutableComponent row = TextUtil.empty()
                .append(TextUtil.accent(mod.name()))
                .append(TextUtil.body(" " + mod.version()))
                .append(TextUtil.muted("  " + mod.id() + " \u00B7 " + configState));
        if (!Mycel.isEnabled(mod.id())) {
            row.append(TextUtil.warn("  disabled"));
        }
        UpdateResult update = UpdateChecker.lastResult(mod.id());
        if (update != null && update.updateAvailable()) {
            row.append(TextUtil.success("  \u2191 " + update.latestVersion()));
        }
        return TextUtil.item(row);
    }

    /**
     * Styled row for one config value: path, current, constraint, default when changed.
     *
     * @param value the config value
     * @return the row
     */
    public static MutableComponent configLine(ConfigValue<?> value) {
        String current = ConfigStrings.valueToString(value);
        boolean isDefault = value.isDefault();
        MutableComponent row = TextUtil.empty()
                .append(TextUtil.accent(ConfigStrings.pathOf(value)))
                .append(TextUtil.faint(" = "))
                .append(isDefault ? TextUtil.body(current) : TextUtil.infoText(current))
                .append(TextUtil.muted("  " + ConfigStrings.constraintHint(value)));
        if (!isDefault) {
            row.append(TextUtil.muted("  (default " + ConfigStrings.defaultToString(value) + ")"));
        }
        return TextUtil.item(row);
    }
}

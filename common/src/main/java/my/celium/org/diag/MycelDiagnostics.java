package my.celium.org.diag;

import java.util.ArrayList;
import java.util.List;

import my.celium.org.Mycel;
import my.celium.org.config.MycelConfig;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.platform.PlatformService;
import my.celium.org.util.TextUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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

    /** Styled header for {@code /mycel info}: brand + platform + versions. */
    public static MutableComponent headerLine() {
        PlatformService platform = PlatformHolder.get();
        return TextUtil.prefixed(TextUtil.empty()
                .append(TextUtil.primary("Mycel " + Mycel.getVersion()))
                .append(TextUtil.muted(" on " + platform.platform().displayName()
                        + "  \u2022  loader " + platform.loaderVersion()
                        + "  \u2022  MC " + platform.minecraftVersion())));
    }

    /** Styled one-liner for a single registered mod. */
    public static MutableComponent modLine(ModMetadata mod) {
        MycelConfig config = Mycel.getConfig(mod.id());
        String configState = config == null ? "no config" : config.values().size() + " values";
        MutableComponent line = TextUtil.prefixed(TextUtil.empty()
                .append(TextUtil.faint("\u2022 "))
                .append(TextUtil.accent(mod.id()))
                .append(TextUtil.muted(" " + mod.version() + "  [" + configState + "]")));
        if (!Mycel.isEnabled(mod.id())) {
            line.append(TextUtil.empty().append(TextUtil.muted("  ")).append(TextUtil.warn("[disabled]")));
        }
        return line;
    }

    /** Styled detail for one config value: path, current, default, constraint. */
    public static MutableComponent configLine(my.celium.org.config.ConfigValue<?> value) {
        String path = my.celium.org.config.ConfigStrings.pathOf(value);
        String current = my.celium.org.config.ConfigStrings.valueToString(value);
        String constraint = my.celium.org.config.ConfigStrings.constraintHint(value);
        boolean isDefault = value.isDefault();
        MutableComponent line = TextUtil.prefixed(TextUtil.empty()
                .append(TextUtil.faint("\u2022 "))
                .append(TextUtil.accent(path))
                .append(TextUtil.muted(" = "))
                .append(isDefault ? TextUtil.body(current) : TextUtil.infoText(current)));
        line.append(TextUtil.muted("  [" + constraint + "]"));
        if (!isDefault) {
            line.append(TextUtil.muted("  (default: "
                    + my.celium.org.config.ConfigStrings.defaultToString(value) + ")"));
        }
        if (!value.description().isBlank()) {
            line.append(Component.empty());
        }
        return line;
    }
}

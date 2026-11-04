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
     * Row with Mycel's version and update state, like {@code Using Mycel 1.6.0 (Up to date)} or {@code Using Mycel 1.5.0 → 1.6.0}.
     * The state is left out until the update check has an answer (or when it is turned off).
     *
     * @return the row
     */
    public static MutableComponent versionLine() {
        MutableComponent row = TextUtil.empty().append(TextUtil.body("Using Mycel"));
        UpdateResult update = UpdateChecker.lastResult(Mycel.MOD_ID);
        if (update != null && update.updateAvailable()) {
            MutableComponent versions = my.celium.org.internal.UpdateNotices.versions(Mycel.getVersion(), update.latestVersion());
            if (update.url() != null) {
                String url = update.url();
                versions.withStyle(style -> style
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create(url)))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(TextUtil.muted("Open the download page"))));
            }
            row.append(TextUtil.literal(" ")).append(versions);
        } else {
            row.append(TextUtil.body(" " + Mycel.getVersion()));
            if (update != null) {
                row.append(TextUtil.muted(" (Up to date)"));
            }
        }
        return TextUtil.item(row);
    }

    /**
     * Row with the loader and Minecraft version, like {@code Fabric 0.19.5, Minecraft 26.3}.
     *
     * @return the row
     */
    public static MutableComponent platformLine() {
        PlatformService platform = PlatformHolder.get();
        return TextUtil.item(TextUtil.body(platform.platform().displayName() + " " + platform.loaderVersion()
                + ", Minecraft " + platform.minecraftVersion()));
    }

    /**
     * Styled row for a single registered mod, for use under a header.
     *
     * @param mod the mod
     * @return the row: name, version, state and any available update
     */
    public static MutableComponent modLine(ModMetadata mod) {
        MycelConfig config = Mycel.getConfig(mod.id());
        int settings = config == null ? 0 : config.values().size();
        MutableComponent name = TextUtil.hex(mod.name(), TextUtil.ACCENT);
        if (config != null) {
            name.withStyle(style -> style
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand("/mycel get " + mod.id()))
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(TextUtil.muted(
                            mod.id() + ", " + settings + (settings == 1 ? " setting" : " settings")
                                    + ". Click to show them."))));
        }
        UpdateResult update = UpdateChecker.lastResult(mod.id());
        MutableComponent row = TextUtil.empty().append(name);
        if (update != null && update.updateAvailable()) {
            row.append(TextUtil.literal("  ")).append(my.celium.org.internal.UpdateNotices.versions(mod.version(), update.latestVersion()));
        } else {
            row.append(TextUtil.muted("  " + mod.version()));
        }
        if (!Mycel.isEnabled(mod.id())) {
            row.append(TextUtil.warn("   off"));
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
        String hint = ConfigStrings.constraintHint(value);
        MutableComponent row = TextUtil.empty()
                .append(TextUtil.body(ConfigStrings.pathOf(value) + "  "))
                .append(TextUtil.hex(current, TextUtil.ACCENT).withStyle(style -> style.withBold(!isDefault)));
        String after = isDefault ? hint : "default " + ConfigStrings.defaultToString(value) + (hint.isBlank() ? "" : ", " + hint);
        if (!after.isBlank()) {
            row.append(TextUtil.faint("   " + after));
        }
        return TextUtil.item(row);
    }
}

package my.celium.org.command;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import my.celium.org.Mycel;
import my.celium.org.config.ConfigStrings;
import my.celium.org.config.ConfigValue;
import my.celium.org.config.MycelConfig;
import my.celium.org.diag.MycelDiagnostics;
import my.celium.org.diag.MycelDump;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.logging.MycelLog;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.util.TextUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The shared {@code /mycel} command, registered by every loader module through
 * its native command event:
 *
 * <ul>
 *   <li>{@code /mycel help} - usage overview;</li>
 *   <li>{@code /mycel info} - library, platform and environment summary;</li>
 *   <li>{@code /mycel mods} - every mod registered with Mycel;</li>
 *   <li>{@code /mycel reload <modid>} - re-reads that mod's config file (op level 2);</li>
 *   <li>{@code /mycel get <modid> [key]} - prints one or all config values;</li>
 *   <li>{@code /mycel set <modid> <key> <value>} - changes and saves a config
 *       value live (op level 2, validated like the config screen);</li>
 *   <li>{@code /mycel reset <modid> [key]} - resets one or all values to
 *       defaults (op level 2).</li>
 *   <li>{@code /mycel dump} - shows what a support dump contains;
 *       {@code /mycel dump confirm} saves it and uploads it to pastes.dev
 *       (op level 2, or the singleplayer host).</li>
 * </ul>
 *
 * <p>Keys are {@code key} for root values or {@code category.key} for
 * categorised ones. A bare key works when it is unique across categories.
 *
 * @hidden
 */
public final class MycelRootCommand {
    private MycelRootCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        SuggestionProvider<CommandSourceStack> modIds = (context, builder) -> {
            for (ModMetadata mod : Mycel.getRegisteredMods()) {
                builder.suggest(mod.id());
            }
            return builder.buildFuture();
        };

        SuggestionProvider<CommandSourceStack> valuePaths = (context, builder) -> {
            MycelConfig config = configFromContext(context, "modid");
            if (config != null) {
                String remaining = builder.getRemainingLowerCase();
                for (ConfigValue<?> value : config.values()) {
                    String path = ConfigStrings.pathOf(value);
                    if (path.toLowerCase(java.util.Locale.ROOT).startsWith(remaining)) {
                        builder.suggest(path);
                    }
                }
            }
            return builder.buildFuture();
        };

        SuggestionProvider<CommandSourceStack> valueGuesses = (context, builder) -> {
            MycelConfig config = configFromContext(context, "modid");
            ConfigValue<?> value = config == null ? null : valueFromContext(context, config, "path");
            if (value instanceof ConfigValue.BooleanValue) {
                builder.suggest("true");
                builder.suggest("false");
            } else if (value instanceof ConfigValue.EnumValue<?> enumValue) {
                for (Object constant : enumValue.type().getEnumConstants()) {
                    builder.suggest(String.valueOf(constant));
                }
            } else if (value != null) {
                builder.suggest(ConfigStrings.valueToString(value));
                builder.suggest(ConfigStrings.defaultToString(value));
            }
            return builder.buildFuture();
        };

        dispatcher.register(Commands.literal("mycel")
                .requires(Commands.hasPermission(Commands.LEVEL_ALL))
                .executes(context -> {
                    sendHelp(context.getSource());
                    my.celium.org.internal.stats.MycelStats.count("command_help");
                    return 1;
                })
                .then(Commands.literal("help")
                        .executes(context -> {
                            sendHelp(context.getSource());
                            my.celium.org.internal.stats.MycelStats.count("command_help");
                            return 1;
                        }))
                .then(Commands.literal("info")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            CommandHelper.success(source, TextUtil.header("Info"));
                            CommandHelper.success(source, MycelDiagnostics.versionLine());
                            CommandHelper.success(source, MycelDiagnostics.platformLine());
                            CommandHelper.success(source, TextUtil.item(TextUtil.keyValue("Mods",
                                    String.valueOf(Mycel.getRegisteredMods().size())))
                                    .append(TextUtil.literal("   "))
                                    .append(TextUtil.button("Show", "/mycel mods", "List the mods using Mycel")));
                            return 1;
                        }))
                .then(Commands.literal("mods")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            List<ModMetadata> mods = List.copyOf(Mycel.getRegisteredMods());
                            if (mods.isEmpty()) {
                                CommandHelper.success(source,
                                        TextUtil.note("No mods use Mycel yet."));
                            } else {
                                CommandHelper.success(source,
                                        TextUtil.header("Mods", String.valueOf(mods.size())));
                                for (ModMetadata mod : mods) {
                                    CommandHelper.success(source, MycelDiagnostics.modLine(mod));
                                }
                            }
                            return Math.max(1, mods.size());
                        }))
                .then(Commands.literal("reload")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("modid", StringArgumentType.word())
                                .suggests(modIds)
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    String modId = StringArgumentType.getString(context, "modid");
                                    MycelConfig config = Mycel.getConfig(modId);
                                    if (config == null) {
                                        CommandHelper.failure(source, noConfig(modId));
                                        return 0;
                                    }
                                    config.reload();
                                    my.celium.org.internal.stats.MycelStats.count("command_reload");
                                    CommandHelper.success(source, TextUtil.ok("Reloaded the settings of ",
                                            TextUtil.hex(displayName(modId), TextUtil.ACCENT), "."));
                                    return 1;
                                })))
                .then(Commands.literal("get")
                        .then(Commands.argument("modid", StringArgumentType.word())
                                .suggests(modIds)
                                .executes(context -> listValues(context.getSource(),
                                        StringArgumentType.getString(context, "modid")))
                                .then(Commands.argument("path", StringArgumentType.word())
                                        .suggests(valuePaths)
                                        .executes(context -> showValue(context.getSource(),
                                                StringArgumentType.getString(context, "modid"),
                                                StringArgumentType.getString(context, "path"))))))
                .then(Commands.literal("set")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("modid", StringArgumentType.word())
                                .suggests(modIds)
                                .then(Commands.argument("path", StringArgumentType.word())
                                        .suggests(valuePaths)
                                        .then(Commands.argument("value",
                                                StringArgumentType.greedyString())
                                                .suggests(valueGuesses)
                                                .executes(context -> setValue(context.getSource(),
                                                        StringArgumentType.getString(context, "modid"),
                                                        StringArgumentType.getString(context, "path"),
                                                        StringArgumentType.getString(context,
                                                                "value")))))))
                .then(Commands.literal("reset")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("modid", StringArgumentType.word())
                                .suggests(modIds)
                                .executes(context -> resetValues(context.getSource(),
                                        StringArgumentType.getString(context, "modid"), null))
                                .then(Commands.argument("path", StringArgumentType.word())
                                        .suggests(valuePaths)
                                        .executes(context -> resetValues(context.getSource(),
                                                StringArgumentType.getString(context, "modid"),
                                                StringArgumentType.getString(context, "path"))))))
                .then(Commands.literal("dump")
                        .requires(MycelRootCommand::canDump)
                        .executes(context -> previewDump(context.getSource()))
                        .then(Commands.literal("confirm")
                                .executes(context -> runDump(context.getSource())))));
    }

    // ------------------------------------------------------------------ help

    private static void sendHelp(CommandSourceStack source) {
        CommandHelper.success(source, TextUtil.header("Commands"));
        CommandHelper.success(source, MycelDiagnostics.versionLine());
        CommandHelper.success(source, usage("/mycel info", "versions"));
        CommandHelper.success(source, usage("/mycel mods", "the mods using Mycel"));
        CommandHelper.success(source, usage("/mycel get <mod> [setting]", "show settings"));
        CommandHelper.success(source, usage("/mycel set <mod> <setting> <value>", "change a setting (op)"));
        CommandHelper.success(source, usage("/mycel reset <mod> [setting]", "back to defaults (op)"));
        CommandHelper.success(source, usage("/mycel reload <mod>", "re-read the config file (op)"));
        CommandHelper.success(source, usage("/mycel dump", "support report to share (op)"));
        CommandHelper.success(source, TextUtil.detail("Click a command to type it."));
    }

    private static Component usage(String command, String blurb) {
        int placeholder = command.replace('[', '<').indexOf(" <");
        String typed = placeholder < 0 ? command : command.substring(0, placeholder + 1);
        return TextUtil.item(TextUtil.empty()
                .append(TextUtil.body(command).withStyle(style -> style
                        .withClickEvent(new ClickEvent.SuggestCommand(typed))
                        .withHoverEvent(new HoverEvent.ShowText(TextUtil.muted("Click to type it")))))
                .append(TextUtil.muted(" - " + blurb)));
    }

    /** A mod's display name, or its id when it is not registered. */
    private static String displayName(String modId) {
        for (ModMetadata mod : Mycel.getRegisteredMods()) {
            if (mod.id().equals(modId)) {
                return mod.name();
            }
        }
        return modId;
    }

    // ------------------------------------------------------------------ get

    private static int listValues(CommandSourceStack source, String modId) {
        MycelConfig config = Mycel.getConfig(modId);
        if (config == null) {
            CommandHelper.failure(source, noConfig(modId));
            return 0;
        }
        if (config.values().isEmpty()) {
            CommandHelper.success(source, TextUtil.note(displayName(modId) + " has no settings."));
            return 1;
        }
        CommandHelper.success(source, TextUtil.header(displayName(modId),
                config.values().size() + (config.values().size() == 1 ? " setting" : " settings")));
        for (ConfigValue<?> value : config.values()) {
            CommandHelper.success(source, MycelDiagnostics.configLine(value));
            if (!value.description().isBlank()) {
                CommandHelper.success(source, TextUtil.detail(value.description()));
            }
        }
        return config.values().size();
    }

    private static int showValue(CommandSourceStack source, String modId, String path) {
        MycelConfig config = Mycel.getConfig(modId);
        if (config == null) {
            CommandHelper.failure(source, noConfig(modId));
            return 0;
        }
        ConfigValue<?> value = ConfigStrings.find(config, path);
        if (value == null) {
            CommandHelper.failure(source, unknownKey(config, path));
            return 0;
        }
        CommandHelper.success(source, TextUtil.header(ConfigStrings.pathOf(value), displayName(modId)));
        CommandHelper.success(source, TextUtil.item(TextUtil.keyValue("Value  ",
                TextUtil.hex(ConfigStrings.valueToString(value), TextUtil.ACCENT))));
        CommandHelper.success(source, TextUtil.item(TextUtil.keyValue("Default", ConfigStrings.defaultToString(value))));
        CommandHelper.success(source, TextUtil.item(TextUtil.keyValue("Allowed", ConfigStrings.describe(value))));
        if (!value.description().isBlank()) {
            CommandHelper.success(source, TextUtil.detail(value.description()));
        }
        return 1;
    }

    // ------------------------------------------------------------------ set

    private static int setValue(CommandSourceStack source, String modId, String path, String rawValue) {
        MycelConfig config = Mycel.getConfig(modId);
        if (config == null) {
            CommandHelper.failure(source, noConfig(modId));
            return 0;
        }
        ConfigValue<?> value = ConfigStrings.find(config, path);
        if (value == null) {
            CommandHelper.failure(source, unknownKey(config, path));
            return 0;
        }
        String before = ConfigStrings.valueToString(value);
        my.celium.org.internal.stats.MycelStats.count("command_set");
        try {
            ConfigStrings.parseAndSet(value, rawValue);
        } catch (IllegalArgumentException e) {
            CommandHelper.failure(source, TextUtil.fail("'" + rawValue.trim() + "' does not work for "
                    + ConfigStrings.pathOf(value) + ": " + e.getMessage()));
            CommandHelper.failure(source, TextUtil.detail("Allowed: " + ConfigStrings.describe(value)
                    + ". It stays " + before + "."));
            return 0;
        }
        String after = ConfigStrings.valueToString(value);
        if (before.equals(after)) {
            CommandHelper.success(source, TextUtil.note(ConfigStrings.pathOf(value) + " is already " + after + "."));
            return 1;
        }
        CommandHelper.success(source, TextUtil.ok(ConfigStrings.pathOf(value) + " is now ",
                TextUtil.hex(after, TextUtil.ACCENT), TextUtil.muted("   was " + before)));
        return 1;
    }

    // ---------------------------------------------------------------- reset

    private static int resetValues(CommandSourceStack source, String modId, String path) {
        MycelConfig config = Mycel.getConfig(modId);
        if (config == null) {
            CommandHelper.failure(source, noConfig(modId));
            return 0;
        }
        if (path == null) {
            config.resetAll();
            CommandHelper.success(source,
                    TextUtil.ok("All settings of ", TextUtil.hex(displayName(modId), TextUtil.ACCENT),
                            " are back to their defaults."));
            return Math.max(1, config.values().size());
        }
        ConfigValue<?> value = ConfigStrings.find(config, path);
        if (value == null) {
            CommandHelper.failure(source, unknownKey(config, path));
            return 0;
        }
        value.reset();
        CommandHelper.success(source, TextUtil.ok(ConfigStrings.pathOf(value) + " is back to ",
                TextUtil.hex(ConfigStrings.defaultToString(value), TextUtil.ACCENT), "."));
        return 1;
    }

    // ---------------------------------------------------------------- dump

    /** Ops, the console, and the singleplayer host (who has no op level without cheats). */
    private static boolean canDump(CommandSourceStack source) {
        if (Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)) {
            return true;
        }
        ServerPlayer player = source.getPlayer();
        return player != null && source.getServer() != null
                && source.getServer().isSingleplayerOwner(player.nameAndId());
    }

    private static int previewDump(CommandSourceStack source) {
        int mods = PlatformHolder.get().loadedMods().size();
        CommandHelper.success(source, TextUtil.header("Support dump"));
        CommandHelper.success(source, TextUtil.item(TextUtil.body("Versions, " + mods + " mods, the settings of "
                + Mycel.getRegisteredMods().size() + " Mycel mods and the end of your log.")));
        CommandHelper.success(source, TextUtil.detail("IP addresses, tokens and your home folder are removed."));
        if (MycelDump.uploadsEnabled()) {
            CommandHelper.success(source, TextUtil.detail("It goes to pastes.dev: anyone with the link can read it."));
            CommandHelper.success(source, TextUtil.item(TextUtil.button("Upload", "/mycel dump confirm",
                    "Save the dump and upload it")));
        } else {
            CommandHelper.success(source, TextUtil.detail("Uploads are off in mycel.json, so it is only saved to mycel-dumps/."));
            CommandHelper.success(source, TextUtil.item(TextUtil.button("Save", "/mycel dump confirm",
                    "Save the dump to mycel-dumps/")));
        }
        return 1;
    }

    private static int runDump(CommandSourceStack source) {
        long wait = MycelDump.cooldownSeconds();
        if (MycelDump.uploadsEnabled() && wait > 0) {
            CommandHelper.failure(source, TextUtil.caution("You just uploaded one. Try again in " + wait + " s."));
            return 0;
        }
        String json = MycelDump.toJson(MycelDump.collect(source.getServer()));
        Path saved;
        try {
            saved = MycelDump.save(json);
        } catch (IOException e) {
            saved = null;
        }
        String local = saved == null ? null : "mycel-dumps/" + saved.getFileName();
        if (!MycelDump.uploadsEnabled()) {
            if (local == null) {
                CommandHelper.failure(source, TextUtil.fail("Could not save the dump. The log says why."));
                return 0;
            }
            CommandHelper.success(source, TextUtil.ok("Saved to " + local + "."));
            return 1;
        }
        CommandHelper.success(source, TextUtil.note("Uploading the dump..."));
        MinecraftServer server = source.getServer();
        MycelDump.upload(json).whenComplete((key, error) -> server.execute(() -> {
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                MycelLog.warn("Dump upload failed: {}", cause.toString());
                CommandHelper.failure(source, TextUtil.fail("The upload failed (" + shortReason(cause) + ")."
                        + (local == null ? "" : " It is saved to " + local + " instead.")));
                return;
            }
            String url = MycelDump.viewerUrl(key);
            my.celium.org.internal.stats.MycelStats.count("dump_upload");
            CommandHelper.success(source, TextUtil.ok("Dump ready   ", TextUtil.link("Open", url), "   ",
                    TextUtil.copy("Copy link", url)));
            MycelLog.info("Dump uploaded: {}", url);
        }));
        return 1;
    }

    private static String shortReason(Throwable cause) {
        String message = cause.getMessage();
        if (message == null || message.isBlank() || message.length() > 60) {
            return cause.getClass().getSimpleName();
        }
        return message;
    }

    // ---------------------------------------------------------------- errors

    private static Component noConfig(String modId) {
        return TextUtil.fail("No Mycel mod called '" + modId + "'. /mycel mods lists them.");
    }

    private static Component unknownKey(MycelConfig config, String path) {
        List<String> matches = new ArrayList<>();
        for (ConfigValue<?> value : config.values()) {
            if (value.key().equalsIgnoreCase(path.trim())) {
                matches.add(ConfigStrings.pathOf(value));
            }
        }
        if (matches.size() > 1) {
            return TextUtil.fail("'" + path + "' matches more than one setting: " + String.join(", ", matches) + ".");
        }
        List<String> available = new ArrayList<>();
        for (ConfigValue<?> value : config.values()) {
            available.add(ConfigStrings.pathOf(value));
        }
        String close = closest(path, available);
        if (close != null) {
            return TextUtil.fail("No setting called '" + path + "'. Did you mean " + close + "?");
        }
        return TextUtil.fail("No setting called '" + path + "'."
                + (available.isEmpty() ? "" : " Settings: " + String.join(", ", available) + "."));
    }

    /** The setting name nearest to a typo, or null when none is close. */
    private static String closest(String typed, List<String> names) {
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        String t = typed.trim().toLowerCase(java.util.Locale.ROOT);
        for (String name : names) {
            String n = name.toLowerCase(java.util.Locale.ROOT);
            String leaf = n.substring(n.lastIndexOf('.') + 1);
            int d = Math.min(distance(t, n), distance(t, leaf));
            if (d < bestDistance) {
                bestDistance = d;
                best = name;
            }
        }
        return best != null && bestDistance <= Math.max(1, t.length() / 3) ? best : null;
    }

    private static int distance(String a, String b) {
        int[] row = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            row[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            int previous = row[0];
            row[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int temp = row[j];
                row[j] = Math.min(Math.min(row[j] + 1, row[j - 1] + 1), previous + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
                previous = temp;
            }
        }
        return row[b.length()];
    }

    private static MycelConfig configFromContext(CommandContext<CommandSourceStack> context, String name) {
        try {
            String modId = context.getArgument(name, String.class);
            return Mycel.getConfig(modId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ConfigValue<?> valueFromContext(CommandContext<CommandSourceStack> context,
            MycelConfig config, String name) {
        try {
            String path = context.getArgument(name, String.class);
            return ConfigStrings.find(config, path);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package my.celium.org.command;

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
import my.celium.org.metadata.ModMetadata;
import my.celium.org.util.TextUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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
 * </ul>
 *
 * <p>Keys are {@code key} for root values or {@code category.key} for
 * categorised ones. A bare key works when it is unique across categories.
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
                .then(Commands.literal("help")
                        .executes(context -> {
                            sendHelp(context.getSource());
                            return 1;
                        }))
                .then(Commands.literal("info")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            CommandHelper.success(source, MycelDiagnostics.headerLine());
                            CommandHelper.success(source, TextUtil.prefixed(TextUtil.empty()
                                    .append(TextUtil.muted("Registered mods ("))
                                    .append(TextUtil.body(
                                            String.valueOf(Mycel.getRegisteredMods().size())))
                                    .append(TextUtil.muted(") \u2014 see /mycel mods"))));
                            return 1;
                        }))
                .then(Commands.literal("mods")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            List<ModMetadata> mods = List.copyOf(Mycel.getRegisteredMods());
                            if (mods.isEmpty()) {
                                CommandHelper.success(source,
                                        TextUtil.note("No mods registered with Mycel yet."));
                            } else {
                                CommandHelper.success(source,
                                        TextUtil.header("Mods (" + mods.size() + ")"));
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
                                        CommandHelper.success(source, TextUtil.fail(
                                                "No Mycel config for '" + modId + "'."));
                                        return 0;
                                    }
                                    config.reload();
                                    CommandHelper.success(source, TextUtil.ok(
                                            "Reloaded '" + modId + "' (" + config.values().size()
                                                    + " values)."));
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
                                                StringArgumentType.getString(context, "path")))))));
    }

    // ------------------------------------------------------------------ help

    private static void sendHelp(CommandSourceStack source) {
        CommandHelper.success(source, TextUtil.header("Mycel " + Mycel.getVersion()));
        CommandHelper.success(source, usage("/mycel info", "library, loader and MC versions"));
        CommandHelper.success(source, usage("/mycel mods", "mods registered with Mycel"));
        CommandHelper.success(source,
                usage("/mycel get <mod> [key]", "show one or all config values"));
        CommandHelper.success(source,
                usage("/mycel set <mod> <key> <value>", "change + save a config value (op)"));
        CommandHelper.success(source,
                usage("/mycel reset <mod> [key]", "restore defaults (op)"));
        CommandHelper.success(source, usage("/mycel reload <mod>", "re-read the config file (op)"));
        CommandHelper.success(source, TextUtil.prefixed(TextUtil.empty()
                .append(TextUtil.muted("Keys look like "))
                .append(TextUtil.accent("category.key"))
                .append(TextUtil.muted(" or just "))
                .append(TextUtil.accent("key"))
                .append(TextUtil.muted(" when unique."))));
    }

    private static Component usage(String command, String blurb) {
        return TextUtil.prefixed(TextUtil.empty()
                .append(TextUtil.infoText(command))
                .append(TextUtil.muted("  \u2014  " + blurb)));
    }

    // ------------------------------------------------------------------ get

    private static int listValues(CommandSourceStack source, String modId) {
        MycelConfig config = Mycel.getConfig(modId);
        if (config == null) {
            CommandHelper.success(source, TextUtil.fail("No Mycel config for '" + modId + "'."));
            return 0;
        }
        if (config.values().isEmpty()) {
            CommandHelper.success(source, TextUtil.note("'" + modId + "' has no config values."));
            return 1;
        }
        CommandHelper.success(source, TextUtil.header(modId + "  (" + config.values().size() + " values)"));
        for (ConfigValue<?> value : config.values()) {
            CommandHelper.success(source, MycelDiagnostics.configLine(value));
            if (!value.description().isBlank()) {
                CommandHelper.success(source,
                        TextUtil.prefixed(TextUtil.empty()
                                .append(TextUtil.faint("    \u2514 "))
                                .append(TextUtil.muted(value.description()))));
            }
        }
        return config.values().size();
    }

    private static int showValue(CommandSourceStack source, String modId, String path) {
        MycelConfig config = Mycel.getConfig(modId);
        if (config == null) {
            CommandHelper.success(source, TextUtil.fail("No Mycel config for '" + modId + "'."));
            return 0;
        }
        ConfigValue<?> value = ConfigStrings.find(config, path);
        if (value == null) {
            CommandHelper.success(source, unknownKey(config, path));
            return 0;
        }
        CommandHelper.success(source, MycelDiagnostics.configLine(value));
        CommandHelper.success(source, TextUtil.prefixed(TextUtil.empty()
                .append(TextUtil.muted("Type: "))
                .append(TextUtil.body(ConfigStrings.describe(value)))
                .append(TextUtil.muted("   Default: "))
                .append(TextUtil.body(ConfigStrings.defaultToString(value)))));
        if (!value.description().isBlank()) {
            CommandHelper.success(source, TextUtil.note(value.description()));
        }
        return 1;
    }

    // ------------------------------------------------------------------ set

    private static int setValue(CommandSourceStack source, String modId, String path, String rawValue) {
        MycelConfig config = Mycel.getConfig(modId);
        if (config == null) {
            CommandHelper.success(source, TextUtil.fail("No Mycel config for '" + modId + "'."));
            return 0;
        }
        ConfigValue<?> value = ConfigStrings.find(config, path);
        if (value == null) {
            CommandHelper.success(source, unknownKey(config, path));
            return 0;
        }
        String before = ConfigStrings.valueToString(value);
        try {
            ConfigStrings.parseAndSet(value, rawValue);
        } catch (IllegalArgumentException e) {
            CommandHelper.success(source, TextUtil.fail("Invalid value for '"
                    + ConfigStrings.pathOf(value) + "': " + e.getMessage()));
            CommandHelper.success(source, TextUtil.prefixed(TextUtil.empty()
                    .append(TextUtil.muted("Expected "))
                    .append(TextUtil.body(ConfigStrings.describe(value)))
                    .append(TextUtil.muted("   Current: "))
                    .append(TextUtil.body(before))));
            return 0;
        }
        String after = ConfigStrings.valueToString(value);
        if (before.equals(after)) {
            CommandHelper.success(source, TextUtil.note("'" + ConfigStrings.pathOf(value)
                    + "' is already " + after + "."));
            return 1;
        }
        CommandHelper.success(source, TextUtil.prefixed(TextUtil.empty()
                .append(TextUtil.success("\u2714 "))
                .append(TextUtil.accent(ConfigStrings.pathOf(value)))
                .append(TextUtil.muted("  " + before + "  \u2192  "))
                .append(TextUtil.infoText(after))));
        return 1;
    }

    // ---------------------------------------------------------------- reset

    private static int resetValues(CommandSourceStack source, String modId, String path) {
        MycelConfig config = Mycel.getConfig(modId);
        if (config == null) {
            CommandHelper.success(source, TextUtil.fail("No Mycel config for '" + modId + "'."));
            return 0;
        }
        if (path == null) {
            config.resetAll();
            CommandHelper.success(source,
                    TextUtil.ok("Reset '" + modId + "' to defaults (" + config.values().size() + " values)."));
            return Math.max(1, config.values().size());
        }
        ConfigValue<?> value = ConfigStrings.find(config, path);
        if (value == null) {
            CommandHelper.success(source, unknownKey(config, path));
            return 0;
        }
        value.reset();
        CommandHelper.success(source, TextUtil.ok("Reset '" + ConfigStrings.pathOf(value) + "' to "
                + ConfigStrings.defaultToString(value) + "."));
        return 1;
    }

    // ---------------------------------------------------------------- errors

    private static Component unknownKey(MycelConfig config, String path) {
        List<String> matches = new ArrayList<>();
        for (ConfigValue<?> value : config.values()) {
            if (value.key().equalsIgnoreCase(path.trim())) {
                matches.add(ConfigStrings.pathOf(value));
            }
        }
        if (matches.size() > 1) {
            return TextUtil.fail("Ambiguous key '" + path + "'; use one of: " + String.join(", ", matches));
        }
        List<String> available = new ArrayList<>();
        for (ConfigValue<?> value : config.values()) {
            available.add(ConfigStrings.pathOf(value));
        }
        String hint = available.isEmpty() ? "this config has no values"
                : "available: " + String.join(", ", available);
        return TextUtil.fail("Unknown key '" + path + "' (" + hint + ").");
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

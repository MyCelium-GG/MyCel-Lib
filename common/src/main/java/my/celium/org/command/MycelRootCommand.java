package my.celium.org.command;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import my.celium.org.Mycel;
import my.celium.org.config.MycelConfig;
import my.celium.org.diag.MycelDiagnostics;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.util.TextUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * The shared {@code /mycel} diagnostics command, registered by every loader
 * module through its native command event:
 *
 * <ul>
 *   <li>{@code /mycel info} - library, platform and environment summary;</li>
 *   <li>{@code /mycel mods} - every mod registered with Mycel;</li>
 *   <li>{@code /mycel reload <modid>} - re-reads that mod's config file (op level 2),
 *       with tab-completion over registered mod ids.</li>
 * </ul>
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

        dispatcher.register(Commands.literal("mycel")
                .requires(Commands.hasPermission(Commands.LEVEL_ALL))
                .then(Commands.literal("info")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            for (String line : MycelDiagnostics.summaryLines()) {
                                source.sendSuccess(() -> TextUtil.gray(line), false);
                            }
                            return 1;
                        }))
                .then(Commands.literal("mods")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            List<ModMetadata> mods = List.copyOf(Mycel.getRegisteredMods());
                            if (mods.isEmpty()) {
                                CommandHelper.success(source, TextUtil.gray("No mods registered with Mycel."));
                            } else {
                                for (ModMetadata mod : mods) {
                                    CommandHelper.success(source, TextUtil.gray(" - " + mod.id() + " "
                                            + mod.version() + (Mycel.isEnabled(mod.id()) ? "" : " [disabled]")));
                                }
                            }
                            return mods.size();
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
                                        CommandHelper.success(source,
                                                TextUtil.red("No Mycel config for mod '" + modId + "'."));
                                        return 0;
                                    }
                                    config.reload();
                                    CommandHelper.success(source,
                                            TextUtil.green("Reloaded config for '" + modId + "'."));
                                    return 1;
                                }))));
    }
}

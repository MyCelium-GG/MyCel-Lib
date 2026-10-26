package my.celium.org.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import my.celium.org.util.TextUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/**
 * Small helpers around vanilla Brigadier. Mycel does not replace Minecraft's
 * command system - these cover the three lines every mod rewrites.
 *
 * <pre>{@code
 * dispatcher.register(Commands.literal("pets")
 *         .requires(source -> CommandHelper.permission(source, 0))
 *         .executes(context -> {
 *             ServerPlayer player = CommandHelper.player(context.getSource());   // consoles get an error
 *             CommandHelper.success(context.getSource(), CHAT.ok("You have ", count(player), " pets."));
 *             return 1;
 *         }));
 * }</pre>
 */
public final class CommandHelper {
    private CommandHelper() {
    }

    /**
     * Requirement: operator permission level {@code level} or higher, using
     * vanilla's {@code 0} (all) through {@code 4} (owners) ladder.
     *
     * @param source the command source
     * @param level  the minimum level, {@code 0..4}; higher counts as {@code 4}
     * @return whether {@code source} has it
     */
    public static boolean permission(CommandSourceStack source, int level) {
        return switch (level) {
            case 0 -> Commands.hasPermission(Commands.LEVEL_ALL).test(source);
            case 1 -> Commands.hasPermission(Commands.LEVEL_MODERATORS).test(source);
            case 2 -> Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
            case 3 -> Commands.hasPermission(Commands.LEVEL_ADMINS).test(source);
            default -> Commands.hasPermission(Commands.LEVEL_OWNERS).test(source);
        };
    }

    /**
     * The executing player, or a vanilla "not a player" error for consoles.
     *
     * @param source the command source
     * @return the player
     * @throws CommandSyntaxException when the source is not a player
     */
    public static ServerPlayer player(CommandSourceStack source) throws CommandSyntaxException {
        return source.getPlayerOrException();
    }

    /**
     * Whether the source is a player (as opposed to console/command block).
     *
     * @param source the command source
     * @return {@code true} for a player
     */
    public static boolean isPlayer(CommandSourceStack source) {
        return source.isPlayer();
    }

    /**
     * Sends a success message (appears in chat, not the action bar).
     *
     * @param source  who ran the command
     * @param message the message
     */
    public static void success(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, false);
    }

    /**
     * Sends an informational message. The component is delivered as-is so hex
     * colors and hover text survive (older builds flattened everything to grey).
     *
     * @param source  who ran the command
     * @param message the message
     */
    public static void info(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, false);
    }

    /**
     * Sends a failure message. Goes through vanilla's failure channel, so it
     * still shows when the {@code sendCommandFeedback} game rule is off. Vanilla
     * paints the root legacy red, so the message rides one level down with the
     * wiki body color; parts without their own color never turn {@code §c}.
     *
     * @param source  who ran the command
     * @param message the message
     */
    public static void failure(CommandSourceStack source, Component message) {
        source.sendFailure(TextUtil.empty().append(
                TextUtil.empty().withStyle(Style.EMPTY.withColor(TextUtil.BODY)).append(message)));
    }
}

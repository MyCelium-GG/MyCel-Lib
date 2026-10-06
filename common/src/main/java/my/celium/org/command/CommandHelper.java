package my.celium.org.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Small helpers around vanilla Brigadier. Mycel does not replace Minecraft's
 * command system - these cover the three lines every mod rewrites.
 */
public final class CommandHelper {
    private CommandHelper() {
    }

    /**
     * Requirement: operator permission level {@code level} or higher, using
     * vanilla's {@code 0} (all) through {@code 4} (owners) ladder.
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

    /** The executing player, or a vanilla "not a player" error for consoles. */
    public static ServerPlayer player(CommandSourceStack source) throws CommandSyntaxException {
        return source.getPlayerOrException();
    }

    /** Whether the source is a player (as opposed to console/command block). */
    public static boolean isPlayer(CommandSourceStack source) {
        return source.isPlayer();
    }

    /** Sends a success message (appears in chat, not the action bar). */
    public static void success(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, false);
    }

    /**
     * Sends an informational message. The component is delivered as-is so hex
     * colors and hover text survive (older builds flattened everything to grey).
     */
    public static void info(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, false);
    }

    /** Sends a styled failure message (still a "success" send so it shows in chat). */
    public static void failure(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, false);
    }
}

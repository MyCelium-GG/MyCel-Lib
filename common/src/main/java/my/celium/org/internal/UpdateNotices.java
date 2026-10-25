package my.celium.org.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import my.celium.org.Mycel;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.update.UpdateChecker;
import my.celium.org.update.UpdateResult;
import my.celium.org.util.TextUtil;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Internal: tells operators (and the singleplayer host) once per session which
 * Mycel mods have an update. Not public API.
 */
public final class UpdateNotices {
    private static final Set<UUID> NOTIFIED = ConcurrentHashMap.newKeySet();
    private static volatile boolean enabled = true;

    private UpdateNotices() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /** Server stop: the next session notifies again. */
    public static void clear() {
        NOTIFIED.clear();
    }

    public static void onJoin(ServerPlayer player) {
        if (!enabled || !mayManage(player) || !NOTIFIED.add(player.getUUID())) {
            return;
        }
        List<UpdateResult> updates = available();
        if (updates.isEmpty()) {
            NOTIFIED.remove(player.getUUID());  // results may still arrive; try again next join
            return;
        }
        player.sendSystemMessage(line(updates));
    }

    /** Registered mods whose last check found a newer version, in registration order. */
    public static List<UpdateResult> available() {
        List<UpdateResult> found = new ArrayList<>();
        for (ModMetadata mod : Mycel.getRegisteredMods()) {
            UpdateResult result = UpdateChecker.lastResult(mod.id());
            if (result != null && result.updateAvailable()) {
                found.add(result);
            }
        }
        return found;
    }

    static Component line(List<UpdateResult> updates) {
        MutableComponent names = TextUtil.empty();
        for (int i = 0; i < updates.size(); i++) {
            UpdateResult update = updates.get(i);
            if (i > 0) {
                names.append(TextUtil.faint(", "));
            }
            ModMetadata mod = find(update.modId());
            names.append(TextUtil.accent(mod == null ? update.modId() : mod.name()))
                    .append(TextUtil.infoText(" " + update.latestVersion()));
        }
        return TextUtil.prefixed(TextUtil.msg(
                TextUtil.success("↑ "),
                updates.size() == 1 ? "Update available: " : "Updates available: ",
                names,
                TextUtil.muted("  see /mycel mods")));
    }

    private static ModMetadata find(String modId) {
        for (ModMetadata mod : Mycel.getRegisteredMods()) {
            if (mod.id().equals(modId)) {
                return mod;
            }
        }
        return null;
    }

    private static boolean mayManage(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server != null && server.isSingleplayerOwner(player.nameAndId())) {
            return true;
        }
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
    }
}

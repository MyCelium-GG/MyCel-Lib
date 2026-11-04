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
        for (Component line : lines(updates)) {
            player.sendSystemMessage(line);
        }
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

    /** Header, then one {@code › Name  1.0.0 → 1.1.0} row per update. */
    static List<Component> lines(List<UpdateResult> updates) {
        List<Component> lines = new ArrayList<>();
        lines.add(TextUtil.header(updates.size() == 1 ? "Update ready" : "Updates ready")
                .append(TextUtil.literal("   "))
                .append(TextUtil.button("Show all", "/mycel mods", "See all Mycel mods and their versions")));
        for (UpdateResult update : updates) {
            ModMetadata mod = find(update.modId());
            MutableComponent name = TextUtil.body(mod == null ? update.modId() : mod.name());
            if (update.url() != null) {
                String url = update.url();
                name.withStyle(style -> style
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create(url)))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(
                                TextUtil.muted("Open the download page"))));
            }
            lines.add(TextUtil.item(name.append(TextUtil.literal("  ")).append(versions(update.currentVersion(), update.latestVersion()))));
        }
        return lines;
    }

    /** {@code 1.0.0 → 1.1.0}: the old version grey, the new one white. */
    public static MutableComponent versions(String current, String latest) {
        return TextUtil.empty()
                .append(TextUtil.muted(current))
                .append(TextUtil.faint(" \u2192 "))
                .append(TextUtil.body(latest));
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

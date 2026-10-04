package mycelexample;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client-only helper for the example mod.
 *
 * <p><b>Never referenced from server code paths.</b> It is only invoked from
 * the clientbound packet handler, which never runs on a dedicated server, and
 * classes are resolved lazily — so the mere presence of this file in the
 * common sources is server-safe. This is the same isolation rule Mycel
 * itself follows for {@code my.celium.org.client}.
 */
public final class ClientGreeting {
    private ClientGreeting() {
    }

    public static void show(String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
        }
    }
}

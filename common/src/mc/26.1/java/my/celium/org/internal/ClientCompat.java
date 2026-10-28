package my.celium.org.internal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Client-only counterpart of {@link MinecraftCompat}: vanilla client calls
 * whose signature differs between Minecraft lines. This is the 26.1 copy.
 * Never load it on a dedicated server.
 */
public final class ClientCompat {
    private ClientCompat() {
    }

    // Before 26.2 the screen lives on Minecraft itself, not on Minecraft.gui.
    public static void setScreen(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    public static Screen screen() {
        return Minecraft.getInstance().screen;
    }
}

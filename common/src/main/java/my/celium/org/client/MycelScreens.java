package my.celium.org.client;

import my.celium.org.internal.ClientCompat;
import net.minecraft.client.gui.screens.Screen;

/**
 * Opening and reading the current screen on every Minecraft version Mycel
 * supports. Since 26.2 vanilla does this through {@code Minecraft.gui}, before
 * that through {@code Minecraft} itself; use these instead of either. Client
 * only.
 *
 * <pre>{@code
 * MycelScreens.open(new MyScreen(MycelScreens.current()));
 * }</pre>
 */
public final class MycelScreens {
    private MycelScreens() {
    }

    /**
     * Shows {@code screen}, or closes the open one when it is {@code null}.
     *
     * @param screen the screen to show, or {@code null} to return to the game
     */
    public static void open(Screen screen) {
        ClientCompat.setScreen(screen);
    }

    /**
     * The screen being shown.
     *
     * @return the open screen, or {@code null} while in game
     */
    public static Screen current() {
        return ClientCompat.screen();
    }
}

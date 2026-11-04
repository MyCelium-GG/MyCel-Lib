package my.celium.org.client;

import my.celium.org.internal.stats.MycelStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Client side of Mycel's stats: shows the consent screen once, over the title screen.
 * Reached reflectively from {@code MycelStats}, so servers never load it.
 *
 * @hidden
 */
public final class StatsConsent {
    private static volatile boolean watching;

    private StatsConsent() {
    }

    /** Waits for the title screen on a background thread, then opens the consent screen on the game thread. */
    public static void watch() {
        if (watching) {
            return;
        }
        watching = true;
        Thread thread = new Thread(() -> {
            long until = System.currentTimeMillis() + 15 * 60 * 1000;
            while (System.currentTimeMillis() < until && MycelStats.needsConsent()) {
                if (onTitleScreen()) {
                    Minecraft.getInstance().execute(() -> {
                        if (MycelStats.needsConsent() && MycelScreens.current() instanceof TitleScreen title) {
                            MycelScreens.open(new StatsConsentScreen(title));
                        }
                    });
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            watching = false;
        }, "Mycel stats consent");
        thread.setDaemon(true);
        thread.start();
    }

    /** False while the game is still starting and has no screen manager yet. */
    private static boolean onTitleScreen() {
        try {
            return MycelScreens.current() instanceof TitleScreen;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * The selected game language.
     *
     * @return a code like {@code en_us}
     */
    public static String language() {
        return Minecraft.getInstance().getLanguageManager().getSelected();
    }
}

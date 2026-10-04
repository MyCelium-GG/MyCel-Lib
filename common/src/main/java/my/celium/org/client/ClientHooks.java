package my.celium.org.client;

import my.celium.org.Mycel;
import my.celium.org.config.MycelConfig;
import net.minecraft.client.gui.screens.Screen;

/**
 * Client-only entry points for Mycel's config UI.
 *
 * <p><b>Never reference this class from common/server code</b> - it (transitively)
 * loads {@code net.minecraft.client} classes, which do not exist on dedicated
 * servers. Server-safe callers go through {@code Mycel.openConfigScreen(...)},
 * which uses a reflection-guarded proxy. Loader <em>client</em> entrypoints may
 * call these methods directly.
 */
public final class ClientHooks {
    private ClientHooks() {
    }

    /** Creates the config screen for a live config handle. Client only. */
    public static Screen createConfigScreen(Screen parent, MycelConfig config) {
        if (config == null) {
            return parent;
        }
        return new MycelConfigScreen(parent, config);
    }

    /**
     * Creates the config screen for {@code modId}, or {@code parent} when the
     * mod has no Mycel config. Client only.
     */
    public static Screen createConfigScreen(Object parent, String modId) {
        Screen parentScreen = parent instanceof Screen screen ? screen : null;
        MycelConfig config = modId == null ? null : Mycel.getConfig(modId);
        if (config == null) {
            return parentScreen;
        }
        return new MycelConfigScreen(parentScreen, config);
    }
}

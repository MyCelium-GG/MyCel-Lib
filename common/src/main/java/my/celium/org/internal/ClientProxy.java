package my.celium.org.internal;

import java.lang.reflect.Method;

import my.celium.org.logging.MycelLog;

/**
 * Internal bridge to client-only code.
 *
 * <p>Common and server code must never reference {@code net.minecraft.client}
 * classes directly — doing so would crash dedicated servers at class-load
 * time. This proxy reaches the client screen factory reflectively and only
 * when a client is actually present, so servers never load those classes.
 */
public final class ClientProxy {
    private ClientProxy() {
    }

    /**
     * Opens (creates) the config screen for {@code modId}, or returns
     * {@code null} on a dedicated server / when the mod has no config.
     * The {@code parent} and return value are {@code net.minecraft.client.gui.screens.Screen}
     * instances; {@code Object} keeps this signature free of client types.
     */
    public static Object openConfigScreen(Object parent, String modId) {
        if (!PlatformHolder.get().isClient() || modId == null) {
            return null;
        }
        try {
            Class<?> hooks = Class.forName("my.celium.org.client.ClientHooks");
            Method factory = hooks.getMethod("createConfigScreen", Object.class, String.class);
            return factory.invoke(null, parent, modId);
        } catch (ReflectiveOperationException | RuntimeException e) {
            MycelLog.debug("Could not open config screen for '{}': {}", modId, e.toString());
            return null;
        }
    }
}

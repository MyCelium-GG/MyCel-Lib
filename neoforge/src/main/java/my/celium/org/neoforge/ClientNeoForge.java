package my.celium.org.neoforge;

import my.celium.org.Mycel;
import my.celium.org.client.ClientHooks;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only NeoForge wiring. Only ever touched when the distribution is
 * {@code CLIENT}, so dedicated servers never load it.
 */
public final class ClientNeoForge {
    private ClientNeoForge() {
    }

    /** Adds Mycel's own config screen to the mod list entry. */
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (ModContainer container, Screen parent) -> ClientHooks.createConfigScreen(parent, Mycel.MOD_ID));
    }
}

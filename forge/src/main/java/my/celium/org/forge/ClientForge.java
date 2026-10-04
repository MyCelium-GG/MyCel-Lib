package my.celium.org.forge;

import my.celium.org.Mycel;
import my.celium.org.client.ClientHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Client-only Forge wiring. Only ever touched when
 * {@code FMLEnvironment.dist.isClient()}, so dedicated servers never load it.
 */
public final class ClientForge {
    private ClientForge() {
    }

    /** Adds Mycel's own config screen to the mod list entry. */
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (Minecraft minecraft, Screen parent) -> ClientHooks.createConfigScreen(parent, Mycel.MOD_ID)));
    }
}

package mycelexample.forge;

import my.celium.org.client.ClientHooks;
import mycelexample.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Forge entrypoint for the example mod. */
@Mod(ExampleMod.ID)
public final class ExampleForge {
    public ExampleForge(@SuppressWarnings("unused") FMLJavaModLoadingContext context) {
        ExampleMod.init();
        // Packets registered here are picked up when MycelForge builds its
        // channel during FMLCommonSetupEvent (after every mod constructor).
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (Minecraft minecraft, Screen parent) -> ClientHooks.createConfigScreen(parent, ExampleMod.ID)));
        }
    }
}

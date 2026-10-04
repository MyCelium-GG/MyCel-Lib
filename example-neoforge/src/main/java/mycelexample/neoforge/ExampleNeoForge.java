package mycelexample.neoforge;

import my.celium.org.client.ClientHooks;
import mycelexample.ExampleMod;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** NeoForge entrypoint for the example mod. */
@Mod(ExampleMod.ID)
public final class ExampleNeoForge {
    // modBus/container are required by the loader; Mycel needs neither here.
    public ExampleNeoForge(@SuppressWarnings("unused") IEventBus modBus,
            @SuppressWarnings("unused") ModContainer container) {
        ExampleMod.init();
        // The payload event fires after every mod constructor, so the example's
        // packet (registered above) is picked up by MycelNeoForge's listener.
        if (FMLEnvironment.getDist().isClient()) {
            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                    () -> (ModContainer ignored, Screen parent) -> ClientHooks.createConfigScreen(parent, ExampleMod.ID));
        }
    }
}

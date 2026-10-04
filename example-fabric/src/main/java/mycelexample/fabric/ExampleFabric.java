package mycelexample.fabric;

import my.celium.org.fabric.FabricNetworking;
import mycelexample.ExampleMod;
import net.fabricmc.api.ModInitializer;

/** Fabric entrypoint for the example mod. */
public final class ExampleFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ExampleMod.init();
        // Packets registered after Mycel's entrypoint need a re-flush on Fabric.
        FabricNetworking.flush();
    }
}

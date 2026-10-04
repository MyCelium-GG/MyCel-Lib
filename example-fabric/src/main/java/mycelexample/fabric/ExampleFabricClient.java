package mycelexample.fabric;

import my.celium.org.fabric.FabricNetworking;
import net.fabricmc.api.ClientModInitializer;

/** Fabric client entrypoint for the example mod. */
public final class ExampleFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricNetworking.flushClient();
    }
}

package my.celium.org.fabric;

import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entrypoint: registers clientbound packet handlers.
 * (Mod-menu integration is a one-liner for dependent mods; see the README.
 * Deliberately not a hard dependency here.)
 */
public final class MycelFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricNetworking.flushClient();
    }
}

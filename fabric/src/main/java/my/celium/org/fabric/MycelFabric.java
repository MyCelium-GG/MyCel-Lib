package my.celium.org.fabric;

import my.celium.org.Mycel;
import my.celium.org.command.MycelRootCommand;
import my.celium.org.event.MycelEvents;
import my.celium.org.event.PlayerJoinEvent;
import my.celium.org.event.PlayerLeaveEvent;
import my.celium.org.event.ServerLifecycleEvent;
import my.celium.org.internal.MycelExecutors;
import my.celium.org.internal.ServerHolder;
import my.celium.org.internal.TickScheduler;
import my.celium.org.registry.MycelRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Fabric entrypoint: boots Mycel and forwards loader events into the common hub. */
public final class MycelFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Mycel.initialize();
        MycelRegistry.initBridges(null);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerHolder.set(server);
            MycelEvents.fireServerLifecycle(
                    new ServerLifecycleEvent(server, ServerLifecycleEvent.Phase.STARTED));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            MycelEvents.fireServerLifecycle(
                    new ServerLifecycleEvent(server, ServerLifecycleEvent.Phase.STOPPED));
            ServerHolder.clear();
            TickScheduler.clear();
            MycelExecutors.shutdown();
        });
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> MycelEvents.firePlayerJoin(new PlayerJoinEvent(handler.getPlayer())));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (handler.getPlayer() != null) {
                MycelEvents.firePlayerLeave(new PlayerLeaveEvent(handler.getPlayer()));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> TickScheduler.tick());
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> MycelRootCommand.register(dispatcher));

        FabricNetworking.flush();
    }
}

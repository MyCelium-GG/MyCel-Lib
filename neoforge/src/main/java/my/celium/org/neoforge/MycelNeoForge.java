package my.celium.org.neoforge;

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
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * NeoForge entrypoint: boots Mycel and forwards loader events into the common hub.
 */
@Mod(Mycel.MOD_ID)
public final class MycelNeoForge {
    public MycelNeoForge(IEventBus modBus, ModContainer container) {
        Mycel.initialize();
        MycelRegistry.initBridges(modBus);

        modBus.addListener(this::onPayloads);
        NeoForge.EVENT_BUS.addListener(this::onCommands);
        NeoForge.EVENT_BUS.addListener(this::onLogin);
        NeoForge.EVENT_BUS.addListener(this::onLogout);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);

        if (FMLEnvironment.getDist().isClient()) {
            ClientNeoForge.registerConfigScreen();
        }
    }

    private void onPayloads(RegisterPayloadHandlersEvent event) {
        NeoForgeNetworking.register(event);
    }

    private void onCommands(RegisterCommandsEvent event) {
        MycelRootCommand.register(event.getDispatcher());
    }

    private void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MycelEvents.firePlayerJoin(new PlayerJoinEvent(player));
        }
    }

    private void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MycelEvents.firePlayerLeave(new PlayerLeaveEvent(player));
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        ServerHolder.set(event.getServer());
        MycelEvents.fireServerLifecycle(
                new ServerLifecycleEvent(event.getServer(), ServerLifecycleEvent.Phase.STARTED));
    }

    private void onServerStopped(ServerStoppedEvent event) {
        MycelEvents.fireServerLifecycle(
                new ServerLifecycleEvent(event.getServer(), ServerLifecycleEvent.Phase.STOPPED));
        ServerHolder.clear();
        TickScheduler.clear();
        MycelExecutors.shutdown();
    }

    private void onServerTick(ServerTickEvent.Post event) {
        TickScheduler.tick();
    }
}

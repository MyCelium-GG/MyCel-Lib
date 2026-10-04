package my.celium.org.forge;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Forge entrypoint: boots Mycel and forwards loader events into the common hub.
 */
@Mod(Mycel.MOD_ID)
public final class MycelForge {
    public MycelForge(FMLJavaModLoadingContext context) {
        Mycel.initialize();
        BusGroup modBusGroup = context.getModBusGroup();
        MycelRegistry.initBridges(modBusGroup);
        ForgeNetworking.init(modBusGroup);

        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(this::onLogin);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(this::onLogout);
        ServerStartedEvent.BUS.addListener(this::onServerStarted);
        ServerStoppedEvent.BUS.addListener(this::onServerStopped);
        TickEvent.ServerTickEvent.Post.BUS.addListener(this::onServerTick);
        RegisterCommandsEvent.BUS.addListener(this::onCommands);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientForge.registerConfigScreen();
        }
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

    private void onServerTick(TickEvent.ServerTickEvent.Post event) {
        TickScheduler.tick();
    }
}

package my.celium.org.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import my.celium.org.logging.MycelLog;

/**
 * Tiny loader-independent event hub.
 *
 * <p>Mycel deliberately exposes only a handful of shared events (join, leave,
 * server lifecycle, entity replacement). Everything else should use the
 * loader's native event bus directly - the goal is to hide repetitive loader
 * differences, not to reinvent Minecraft's event systems.
 *
 * <p>All events fire on the server thread. A throwing listener is logged and
 * skipped so one bad handler cannot break the others.
 *
 * <p>Subscribe once, from mod init:
 * <pre>{@code
 * MycelEvents.onPlayerJoin(event -> PlayerUtil.sendMessage(event.player(), CHAT.note("Welcome back!")));
 * MycelEvents.onServerLifecycle(event -> {
 *     if (event.phase() == ServerLifecycleEvent.Phase.STOPPED) {
 *         CACHE.clear();
 *     }
 * });
 * }</pre>
 */
public final class MycelEvents {
    private static final List<Consumer<PlayerJoinEvent>> JOIN = new CopyOnWriteArrayList<>();
    private static final List<Consumer<PlayerLeaveEvent>> LEAVE = new CopyOnWriteArrayList<>();
    private static final List<Consumer<ServerLifecycleEvent>> LIFECYCLE = new CopyOnWriteArrayList<>();
    private static final List<Consumer<EntityReplacedEvent>> REPLACED = new CopyOnWriteArrayList<>();

    private MycelEvents() {
    }

    /**
     * Listens for players joining the server.
     *
     * @param listener called with the event on the server thread
     */
    public static void onPlayerJoin(Consumer<PlayerJoinEvent> listener) {
        JOIN.add(listener);
    }

    /**
     * Listens for players leaving the server.
     *
     * @param listener called with the event on the server thread
     */
    public static void onPlayerLeave(Consumer<PlayerLeaveEvent> listener) {
        LEAVE.add(listener);
    }

    /**
     * Listens for the server starting and stopping.
     *
     * @param listener called with the event on the server thread
     */
    public static void onServerLifecycle(Consumer<ServerLifecycleEvent> listener) {
        LIFECYCLE.add(listener);
    }

    /**
     * Listens for {@code EntityReplacer} swapping one entity for another.
     *
     * @param listener called with the event on the server thread
     */
    public static void onEntityReplaced(Consumer<EntityReplacedEvent> listener) {
        REPLACED.add(listener);
    }

    // Called by the loader modules. Not public API (but must be visible across modules).

    /**
     * Fires the event; called by the loader modules, not by mods.
     *
     * @param event the event
     * @hidden
     */
    public static void firePlayerJoin(PlayerJoinEvent event) {
        fire(JOIN, event, "PlayerJoin");
    }

    /**
     * Fires the event; called by the loader modules, not by mods.
     *
     * @param event the event
     * @hidden
     */
    public static void firePlayerLeave(PlayerLeaveEvent event) {
        fire(LEAVE, event, "PlayerLeave");
    }

    /**
     * Fires the event; called by the loader modules, not by mods.
     *
     * @param event the event
     * @hidden
     */
    public static void fireServerLifecycle(ServerLifecycleEvent event) {
        fire(LIFECYCLE, event, "ServerLifecycle");
    }

    /**
     * Fires the event; called by the loader modules, not by mods.
     *
     * @param event the event
     * @hidden
     */
    public static void fireEntityReplaced(EntityReplacedEvent event) {
        fire(REPLACED, event, "EntityReplaced");
    }

    private static <T> void fire(List<Consumer<T>> listeners, T event, String name) {
        for (Consumer<T> listener : listeners) {
            try {
                listener.accept(event);
            } catch (RuntimeException e) {
                MycelLog.warn("Event listener for {} threw; skipping: {}", name, e.toString());
            }
        }
    }
}

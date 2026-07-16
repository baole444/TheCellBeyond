package eventviewer;

import eventviewer.event.Event;
import scripting.API;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * EngineEventCallback provide static methods for emitting engine event, register and unregister as listener to these event.
 * Classes that implemented the {@link EngineEventListener} interface can handle these event.
 */
@API
public class EngineEventCallback {
    private static final CopyOnWriteArrayList<EngineEventListener> engineEventListeners = new CopyOnWriteArrayList<>();
    private EngineEventCallback() {}

    /**
     * Register a listener instance with the callback for it to receive engine events.
     * If the instance is already registered, this will no nothing.
     * @param listener the listener instance to register
     */
    public static void register(EngineEventListener listener) {
        if (!engineEventListeners.contains(listener)) engineEventListeners.add(listener);
    }

    /**
     * Unregister a listener instance from the callback for it to stop receive engine events.
     * @param listener the listener instance to unregister
     */
    public static void unregister(EngineEventListener listener) {
        engineEventListeners.remove(listener);
    }

    /**
     * Emit an engine event to all listeners.
     * @param event the event to emit
     */
    public static void emit(Event event) {
        emit(null, event);
    }

    /**
     * Emit an engine event to all listeners.
     * @param object the optional extra data to attach with the event
     * @param event the event to emit
     */
    public static void emit(Object object, Event event) {
        for (EngineEventListener engineEventListener : engineEventListeners) {
            if (engineEventListener == null) {
                engineEventListeners.remove(null);
                continue;
            }
            engineEventListener.onEventEmit(object, event);
        }
    }

    /**
     * Unregister all listeners from this callback.
     */
    public static void dispose() {
        engineEventListeners.clear();
    }
}

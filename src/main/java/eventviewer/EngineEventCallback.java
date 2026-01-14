package eventviewer;

import eventviewer.event.Event;

import java.util.concurrent.CopyOnWriteArrayList;

public class EngineEventCallback {
    private static final CopyOnWriteArrayList<EngineEventListener> engineEventListeners = new CopyOnWriteArrayList<>();

    public static void register(EngineEventListener listener) {
        if (!engineEventListeners.contains(listener)) engineEventListeners.add(listener);
    }

    public static void unregister(EngineEventListener listener) {
        engineEventListeners.remove(listener);
    }

    public static void emit(Event event) {
        emit(null, event);
    }

    public static void emit(Object object, Event event) {
        for (EngineEventListener engineEventListener : engineEventListeners) {
            if (engineEventListener == null) {
                engineEventListeners.remove(null);
                continue;
            }

            engineEventListener.onEventEmit(object, event);
        }
    }

    public static void dispose() {
        engineEventListeners.clear();
    }
}

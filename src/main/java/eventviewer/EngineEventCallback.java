package eventviewer;

import eventviewer.event.Event;

import java.util.concurrent.CopyOnWriteArrayList;

public class EngineEventCallback {
    private static final CopyOnWriteArrayList<EngineEventListener> ENGINE_EVENT_LISTENERS = new CopyOnWriteArrayList<>();

    public static void register(EngineEventListener listener) {
        if (!ENGINE_EVENT_LISTENERS.contains(listener)) ENGINE_EVENT_LISTENERS.add(listener);
    }

    public static void unregister(EngineEventListener listener) {
        ENGINE_EVENT_LISTENERS.remove(listener);
    }

    public static void emit(Object object, Event event) {
        for (EngineEventListener engineEventListener : ENGINE_EVENT_LISTENERS) {
            if (engineEventListener == null) {
                ENGINE_EVENT_LISTENERS.remove(null);
                continue;
            }

            engineEventListener.onEventEmit(object, event);
        }
    }
}

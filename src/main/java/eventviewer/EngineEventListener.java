package eventviewer;

import eventviewer.event.Event;

public interface EngineEventListener {
    /**
     * If the implement of {@link EngineEventListener} is registered with {@link EngineEventCallback},
     * this method is called, upon which will execute the implementor's logic.
     * @param object nullable data that the event emitter can pass along with the event
     * @param event the event type
     */
    void onEventEmit(Object object, Event event);

    /**
     * By default, disposing a listener will unregister it from callback.
     */
    default void dispose() {
        EngineEventCallback.unregister(this);
    }
}

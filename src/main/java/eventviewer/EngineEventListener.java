package eventviewer;

import eventviewer.event.Event;

/**
 * The EngineEventListener interface provide the common method {@link #onEventEmit(Object, Event)},
 * which can be implemented to handle emitted engine events.
 * <p>
 * The interface also provide default {@link #register()} and {@link #dispose()} methods,
 * which register and unregister the instance that implemented this interface with {@link EngineEventCallback}.
 */
public interface EngineEventListener {
    /**
     * If the implement of {@link EngineEventListener} is registered with {@link EngineEventCallback},
     * this method is called, upon which will execute the implementor's logic.
     * @param object nullable data that the event emitter can pass along with the event
     * @param event the event type
     */
    void onEventEmit(Object object, Event event);

    /**
     * Be default, register a listener will register it with {@link EngineEventCallback}.
     */
    default void register() {
        EngineEventCallback.register(this);
    }

    /**
     * By default, disposing a listener will unregister it from {@link EngineEventCallback}.
     */
    default void dispose() {
        EngineEventCallback.unregister(this);
    }
}

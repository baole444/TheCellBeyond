package eventviewer;

import eventviewer.event.Event;

public interface EngineEventListener {
    void onEventEmit(Object object, Event event);
}

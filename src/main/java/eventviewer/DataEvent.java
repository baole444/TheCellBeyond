package eventviewer;

import eventviewer.event.Event;

public interface DataEvent {
    void whenNotice(Object data, Event event);
}

package eventviewer;

import eventviewer.event.Event;

public interface IEvent {
    void whenNotice(Object object, Event event);
}

package eventviewer;

import eventviewer.event.Event;

public interface EventInterface {
    void whenNotice(Object object, Event event);
}

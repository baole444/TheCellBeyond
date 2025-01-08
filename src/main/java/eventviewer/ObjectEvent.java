package eventviewer;

import TCB_Field.GameObject;
import eventviewer.event.Event;

public interface ObjectEvent {
    void whenNotice(GameObject object, Event event);
}

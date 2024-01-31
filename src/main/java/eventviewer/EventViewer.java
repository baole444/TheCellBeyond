package eventviewer;

import TCB_Field.GameObject;
import eventviewer.event.Event;

public interface EventViewer {
    void whenNotice(GameObject object, Event event);

}

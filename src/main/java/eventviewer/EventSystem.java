package eventviewer;

import TCB_Field.GameObject;
import eventviewer.event.Event;

import java.util.ArrayList;
import java.util.List;

public class EventSystem {
    private static List<EventViewer> eventViewers = new ArrayList<>();

    public static void addViewer(EventViewer eventViewer) {
        eventViewers.add(eventViewer);
    }

    public static void notice(GameObject object, Event event) {
        for (EventViewer eventViewer : eventViewers) {
            eventViewer.whenNotice(object, event);
        }
    }
}

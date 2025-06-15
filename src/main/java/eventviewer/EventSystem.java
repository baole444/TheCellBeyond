package eventviewer;

import eventviewer.event.Event;

import java.util.ArrayList;
import java.util.List;

public class EventSystem {
    private static final List<EventInterface> eventInterfaces = new ArrayList<>();

    public static void addViewer(EventInterface eventInterface) {
        eventInterfaces.add(eventInterface);
    }

    public static void notice(Object object, Event event) {
        for (EventInterface eventInterface : eventInterfaces) {
            eventInterface.whenNotice(object, event);
        }
    }


}

package eventviewer;

import eventviewer.event.Event;

import java.util.ArrayList;
import java.util.List;

public class EventSystem {
    private static List<IEvent> iEvents = new ArrayList<>();


    public static void addViewer(IEvent iEvent) {
        iEvents.add(iEvent);
    }

    public static void notice(Object object, Event event) {
        for (IEvent iEvent : iEvents) {
            iEvent.whenNotice(object, event);
        }
    }


}

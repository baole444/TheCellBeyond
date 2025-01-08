package eventviewer;

import TCB_Field.GameObject;
import eventviewer.event.Event;

import java.util.ArrayList;
import java.util.List;

public class EventSystem {
    private static List<ObjectEvent> objectEvents = new ArrayList<>();
    private static List<DataEvent> dataEvents = new ArrayList<>();



    public static void addViewer(ObjectEvent objectEvent) {
        objectEvents.add(objectEvent);
    }

    public static void notice(GameObject object, Event event) {
        for (ObjectEvent objectEvent : objectEvents) {
            objectEvent.whenNotice(object, event);
        }
    }

    public static void addViewer(DataEvent dataEvent) {
        dataEvents.add(dataEvent);
    }

    public static void sendData(Object data, Event event) {
        for (DataEvent dataEvent : dataEvents) {
            dataEvent.whenNotice(data, event);
        }
    }
}

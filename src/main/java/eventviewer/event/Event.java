package eventviewer.event;

public class Event {
    public EditorEvent type;

    public Event(EditorEvent type) {
        this.type = type;
    }

    public Event() {
        this.type = EditorEvent.UserEvent;
    }
}

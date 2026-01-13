package eventviewer.event;

public class RuntimeEvent extends Event {
    public enum Type {
        RuntimeStarted,
        RuntimeStopped,
        RuntimeCrashed
    }

    public final Type type;

    public RuntimeEvent(Type type) {
        this.type = type;
    }
}

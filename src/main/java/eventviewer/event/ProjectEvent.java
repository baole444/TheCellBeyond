package eventviewer.event;

/**
 * Possible events emitted by the Project system.
 *
 */
public class ProjectEvent extends Event {
    /**
     * Types of project event.
     */
    public enum Type {
        /**
         * FPS limit changed.
         */
        TargetFrameRateChanged,
        /**
         * Vsync Mode changed.
         */
        VsyncModeChanged,
    }

    /**
     * The type of this project event.
     */
    public final Type type;

    /**
     * Create a new {@link ProjectEvent} using the given type.
     * @param type type of the event
     */
    public ProjectEvent(Type type) {
        this.type = type;
    }
}

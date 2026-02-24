package eventviewer.event;

/**
 * Possible events emitted by the editor and editing process.
 */
public class EditorEvent extends Event {
    /**
     * Types of editor event.
     */
    public enum Type {
        /**
         * Request to load a user project from disk.
         */
        LoadProjectFromDisk,
        /**
         * User project successfully loaded.
         */
        ProjectLoaded,
        /**
         * Request to create a new scene.
         */
        CreateNewScene,
        /**
         * Request to saving the currently editing scene's data.
         */
        SaveEditingSceneToDisk,
        /**
         * Request to load the editing scene from disk.
         */
        LoadEditingSceneFromDisk,
        /**
         * Request to reload a scene's resource.
         */
        ReloadSceneResource,
    }

    /**
     * The type of this editor event.
     */
    public final Type type;

    /**
     * Create a new {@link EditorEvent} using the given type
     * @param type type of the event
     */
    public EditorEvent(Type type) {
        this.type = type;
    }
}

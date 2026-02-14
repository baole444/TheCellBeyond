package eventviewer.event;

public class EditorEvent extends Event {
    public enum Type {
        LoadProjectFromDisk,
        ProjectLoaded,
        CreateNewScene,
        SaveEditingSceneToDisk,
        LoadEditingSceneFromDisk,
        ReloadSceneResource,
    }

    public final Type type;

    public EditorEvent(Type type) {
        this.type = type;
    }
}

package eventviewer.event;

import scene.Scene;
import scripting.API;

import java.util.List;

@API
public class SceneEvent extends Event {
    public enum Type {
        SceneChanging,
        SceneEntered,
        SceneLeaved,
        ObjectAdded,
        ObjectUpdated,
        ObjectRemoved,
        ComponentAdded,
        ComponentRemoved,
    }

    public final Type type;
    public final Scene scene;
    public List<Object> params;

    public SceneEvent(Type type, Scene scene, Object... params) {
        this.type = type;
        this.scene = scene;
        this.params = List.of(params);
    }
}

package scene;

import TheCellBeyond.GameObject;
import TheCellBeyond.Transform2D;
import components.IsNotSelectable;
import editor.components.*;

public class SceneEditor extends SceneLoader {
    private static GameObject levelEditorObject;

    public SceneEditor() {}

    public static void pickUpObject(GameObject pickingObject) {
        if (levelEditorObject == null || levelEditorObject.isDestroyed() || pickingObject == null || pickingObject.isDestroyed()) return;
        levelEditorObject.getFirstComponent(EditorMouseCtrl.class).pickObject(pickingObject);
    }

    @Override
    public void onSceneStarted(Scene scene) {}

    @Override
    public void loadResource(Scene scene) {
        levelEditorObject = new GameObject("EditorObject");
        levelEditorObject.setNotSerialize();
        levelEditorObject.addComponents(new IsNotSelectable(), new Transform2D(),
                new EditorMouseCtrl(), new EditorKeyCtrl(), new EditorGrid(),
                new EditorTileMapGrid(), new EditorTileMapCtrl(),
                new EditorSceneCtrl(scene.viewport()), new EditorGizmoCtrl()
        );
        scene.queueForObjectAddition(levelEditorObject, null);
    }

    @Override
    public void onSceneEnd() {
        levelEditorObject = null;
    }
}

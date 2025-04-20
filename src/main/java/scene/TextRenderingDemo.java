package scene;

import TCB_Field.GameObject;
import TCB_Field.Prefab;
import components.GizmoControl;
import components.Grid;
import components.KeyCtrl;
import components.MouseCtrl;
import editor.EditorViewport;
import org.joml.Vector4f;
import utility.AssetsPool;
import utility.Settings;

public class TextRenderingDemo extends SceneInit{
    private GameObject levelEditorObject;

    @Override
    public void init(Scene scene) {
        GameObject testText = Prefab.genText("A", Settings.PATH.CONSOLA, 2, new Vector4f(1, 1, 1, 1));
        testText.transform.position.set(0.0f, 3f);
        scene.addObjToScene(testText);
    }

    @Override
    public void loadResource(Scene scene) {
    }

    @Override
    public void imgui() {}
}

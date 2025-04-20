package scene;

import TCB_Field.GameObject;
import TCB_Field.Prefab;
import org.joml.Vector4f;
import utility.Settings;

public class TextRenderingDemo extends SceneInit{
    @Override
    public void init(Scene scene) {
        GameObject testText = Prefab.genText("Test text rendering", Settings.PATH.CONSOLA, 24, new Vector4f(1, 1, 1, 1));
        testText.transform.position.set(0.5f, 3.0f);
        scene.addObjToScene(testText);
    }

    @Override
    public void loadResource(Scene scene) {}

    @Override
    public void imgui() {}
}

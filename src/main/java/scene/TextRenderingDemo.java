package scene;

import TCB_Field.GameObject;
import TCB_Field.Prefab;
import org.joml.Vector4f;
import utility.AssetsPool;
import utility.Settings;

public class TextRenderingDemo extends SceneInit{
    @Override
    public void init(Scene scene) {
        GameObject testText = Prefab.genText("Test text rendering", Settings.PATH.CONSOLA, 24, new Vector4f(1, 1, 1, 1));
        testText.transform.position.set(0.0f, 0f);
        scene.addObjToScene(testText);
    }

    @Override
    public void loadResource(Scene scene) {
        AssetsPool.loadShader(Settings.PATH.DEFAULT_FONT_SHADER);
    }

    @Override
    public void imgui() {}
}

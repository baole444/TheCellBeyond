package TCB_Field;

import components.SpriteRender;
import org.joml.Vector2f;
import utility.AssetsPool;

public class LevelEditorScene extends Scene {

    public LevelEditorScene() {


    }

    @Override
    public void init() {

        this.viewport = new Viewport(new Vector2f(0, 0)); //View point position

        GameObject placeholder_object = new GameObject("placeholder_object", new Transform(new Vector2f(150, 200), new Vector2f(96, 96)));
        placeholder_object.addComponent(new SpriteRender(AssetsPool.loadTexture("assets/texture/Just_a_placeholder.png")));
        this.addObjToScene(placeholder_object);

        GameObject niko_drawing = new GameObject("niko_drawing", new Transform(new Vector2f(400, 200), new Vector2f(96,96)));
        niko_drawing.addComponent(new SpriteRender(AssetsPool.loadTexture("assets/texture/af.png")));
        this.addObjToScene(niko_drawing);

        loadRes(); //Don't touch
    }

    private  void loadRes() {
        AssetsPool.loadShader("assets/shaders/default.glsl");
    }

    @Override
    public void update(float dt) {


        for (GameObject go : this.gObjects) {
            go.update(dt);
        }

        this.renderer.render();
    }

}

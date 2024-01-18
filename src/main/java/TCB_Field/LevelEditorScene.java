package TCB_Field;

import components.SpriteRender;
import components.SpriteSheet;
import org.joml.Vector2f;
import utility.AssetsPool;

public class LevelEditorScene extends Scene {

    public LevelEditorScene() {


    }

    @Override
    public void init() {
        loadRes(); //Don't touch


        this.viewport = new Viewport(new Vector2f(0, 0)); //View point position

        SpriteSheet sprites = AssetsPool.loadSprite("assets/texture/Main char.png");

        GameObject obj1 = new GameObject("obj1", new Transform(new Vector2f(100, 200), new Vector2f(90, 90)));
        obj1.addComponent(new SpriteRender(sprites.spriteIndex(0)));
        this.addObjToScene(obj1);

        GameObject obj2 = new GameObject("obj2", new Transform(new Vector2f(200, 200), new Vector2f(90,90)));
        obj2.addComponent(new SpriteRender(sprites.spriteIndex(1)));
        this.addObjToScene(obj2);

        GameObject obj3 = new GameObject("obj3", new Transform(new Vector2f(300, 200), new Vector2f(90,90)));
        obj3.addComponent(new SpriteRender(sprites.spriteIndex(2)));
        this.addObjToScene(obj3);
    }

    private  void loadRes() {
        AssetsPool.loadShader("assets/shaders/default.glsl");
        AssetsPool.addSpSheet("assets/texture/Main char.png",
                new SpriteSheet((AssetsPool.loadTexture("assets/texture/Main char.png")),
                        16, 16, 3, 16));
    }

    @Override
    public void update(float dt) {


        for (GameObject go : this.gObjects) {
            go.update(dt);
        }

        this.renderer.render();
    }

}

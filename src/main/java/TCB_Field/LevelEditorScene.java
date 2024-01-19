package TCB_Field;

import components.SpriteRender;
import components.SpriteSheet;
import org.joml.Vector2f;
import utility.AssetsPool;

public class LevelEditorScene extends Scene {
    private GameObject obj1, obj2, obj3;
    private SpriteSheet sprites;
    public LevelEditorScene() {

    }

    @Override
    public void init() {
        loadRes(); //Don't touch


        this.viewport = new Viewport(new Vector2f(0, 0)); //View point position

        sprites = AssetsPool.loadSpSheet("assets/texture/Main char.png");

        obj1 = new GameObject("obj1", new Transform(new Vector2f(100, 200), new Vector2f(90, 90)), 0);
        obj1.addComponent(new SpriteRender(sprites.spriteIndex(0)));
        this.addObjToScene(obj1);

        obj2 = new GameObject("obj2", new Transform(new Vector2f(280, 200), new Vector2f(90,90)), 1);
        obj2.addComponent(new SpriteRender(sprites.spriteIndex(4)));
        this.addObjToScene(obj2);

        obj3 = new GameObject("obj3", new Transform(new Vector2f(450, 200), new Vector2f(90,90)), 2);
        obj3.addComponent(new SpriteRender(sprites.spriteIndex(8)));
        this.addObjToScene(obj3);
    }

    private  void loadRes() {
        AssetsPool.loadShader("assets/shaders/default.glsl");
        AssetsPool.addSpSheet("assets/texture/Main char.png",
                new SpriteSheet((AssetsPool.loadTexture("assets/texture/Main char.png")),
                        16, 16, 12, 16));
    }

    private int spriteIndex = 0;
    private float spriteFlipTime = 0.5f;
    private float spriteFlipTimeLeft = 0.0f;

    @Override
    public void update(float dt) {
        spriteFlipTimeLeft -= dt;
        if (spriteFlipTimeLeft <= 0) {
            spriteFlipTimeLeft = spriteFlipTime;
            spriteIndex++;
            if(spriteIndex > 3) {
                spriteIndex = 0;
            }
            obj1.getComponent(SpriteRender.class).setSprite(sprites.spriteIndex(spriteIndex));
            obj2.getComponent(SpriteRender.class).setSprite(sprites.spriteIndex(spriteIndex + 4));
            obj3.getComponent(SpriteRender.class).setSprite(sprites.spriteIndex(spriteIndex + 8));
        }

        for (GameObject go : this.gObjects) {
            go.update(dt);
        }

        this.renderer.render();
    }

}

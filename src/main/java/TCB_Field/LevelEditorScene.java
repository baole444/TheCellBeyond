package TCB_Field;

import components.HardObject;
import components.Sprite;
import components.SpriteRender;
import components.SpriteSheet;
import imgui.ImGui;
import org.joml.Vector2f;
import org.joml.Vector4f;
import utility.AssetsPool;

public class LevelEditorScene extends Scene {
    private boolean showText = false;
    private GameObject obj1, obj2, obj3;
    private SpriteSheet sprites;
    private SpriteRender obj3Sprite;
    public LevelEditorScene() {

    }

    @Override
    public void init() {
        loadRes(); //Don't touch
        this.viewport = new Viewport(new Vector2f(0, 0)); //View point position

        if (isLoaded) {
            this.activeGameObject = gObjects.get(2);
            return;
        }

        sprites = AssetsPool.loadSpSheet("assets/texture/Main char.png");

        obj1 = new GameObject("obj1",
                new Transform(new Vector2f(100, 200),
                        new Vector2f(90, 90)), 0);
        SpriteRender obj1Sprite = new SpriteRender();
        obj1Sprite.setSprite(sprites.spriteIndex(0));
        obj1.addComponent(obj1Sprite);
        this.addObjToScene(obj1);


        obj2 = new GameObject("obj2",
                new Transform(new Vector2f(280, 200),
                        new Vector2f(90,90)), 1);
        SpriteRender obj2Sprite = new SpriteRender();
        Sprite obj2Tex = new Sprite();
        obj2Tex.setTex(AssetsPool.loadTexture("assets/texture/Just_a_placeholder.png"));
        obj2Sprite.setSprite(obj2Tex);
        obj2.addComponent(obj2Sprite);
        this.addObjToScene(obj2);

        obj3 = new GameObject("obj3",
                new Transform(new Vector2f(450, 200),
                        new Vector2f(90,90)), 2);
        obj3Sprite = new SpriteRender();
        obj3Sprite.setColor(new Vector4f(1, 1 , 1, 1));
        obj3.addComponent(obj3Sprite);
        obj3.addComponent(new HardObject());
        this.addObjToScene(obj3);

    }

    private void loadRes() {
        AssetsPool.loadShader("assets/shaders/default.glsl");
        AssetsPool.addSpSheet("assets/texture/Main char.png",
                new SpriteSheet((AssetsPool.loadTexture("assets/texture/Main char.png")),
                        16, 16, 12, 16)
        );
        AssetsPool.loadTexture("assets/texture/Just_a_placeholder.png");
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
            //obj1.getComponent(SpriteRender.class).setSprite(sprites.spriteIndex(spriteIndex));
        }

        for (GameObject go : this.gObjects) {
            go.update(dt);
        }

        this.renderer.render();
    }

    @Override
    public void imgui() {
        ImGui.begin("Test");

        if (ImGui.button("test button")) {
            showText = true;
        }

        if (showText) {
            ImGui.text("button registered");
            ImGui.sameLine();
            if (ImGui.button("End text")) {
                showText = false;
            }
        }

        ImGui.end();
    }

}

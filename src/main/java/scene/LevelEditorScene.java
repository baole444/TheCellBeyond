package scene;

import TCB_Field.GameObject;
import TCB_Field.Prefab;
import TCB_Field.Transform;
import TCB_Field.Viewport;
import components.*;
import imgui.ImGui;
import imgui.ImVec2;
import org.joml.Vector2f;
import org.joml.Vector3f;
import render.DebugDraw;
import utility.AssetsPool;

public class LevelEditorScene extends Scene {
    private boolean showText = false;
    private GameObject obj1, obj2, obj3;
    private SpriteSheet sprites;
    private SpriteRender obj3Sprite;

    GameObject levelEditorObject = new GameObject("Lvl Editor", new Transform(new Vector2f()), 0);
    public LevelEditorScene() {

    }

    @Override
    public void init() {
        levelEditorObject.addComponent(new MouseCtrl());
        levelEditorObject.addComponent(new Grid());

        loadRes(); //Don't touch
        this.viewport = new Viewport(new Vector2f(0, 0)); //View point position
        sprites = AssetsPool.loadSpSheet("assets/texture/Main char.png");
        if (isLoaded) {
            //this.activeGameObject = gObjects.get(0);
            return;
        }

//        obj1 = new GameObject("obj1",
//                new Transform(new Vector2f(100, 200),
//                        new Vector2f(90, 90)), 0);
//        SpriteRender obj1Sprite = new SpriteRender();
//        obj1Sprite.setSprite(sprites.spriteIndex(0));
//        obj1.addComponent(obj1Sprite);
//        this.addObjToScene(obj1);
//
//
//        obj2 = new GameObject("obj2",
//                new Transform(new Vector2f(280, 200),
//                        new Vector2f(90,90)), 0);
//        SpriteRender obj2Sprite = new SpriteRender();
//        Sprite obj2Tex = new Sprite();
//        obj2Tex.setTex(AssetsPool.loadTexture("assets/texture/Just_a_placeholder.png"));
//        obj2Sprite.setSprite(obj2Tex);
//        obj2.addComponent(obj2Sprite);
//        this.addObjToScene(obj2);
//
//        obj3 = new GameObject("obj3",
//                new Transform(new Vector2f(450, 200),
//                        new Vector2f(90,90)), 0);
//        obj3Sprite = new SpriteRender();
//        obj3Sprite.setColor(new Vector4f(1, 1 , 1, 1));
//        obj3.addComponent(obj3Sprite);
//        obj3.addComponent(new HardObject());
//        this.addObjToScene(obj3);


    }

    private void loadRes() {
        AssetsPool.loadShader("assets/shaders/default.glsl");
        AssetsPool.addSpSheet("assets/texture/Main char.png",
                new SpriteSheet((AssetsPool.loadTexture("assets/texture/Main char.png")),
                        16, 16, 13, 16)
        );
        AssetsPool.loadTexture("assets/texture/Just_a_placeholder.png");
    }

//    private int spriteIndex = 0;
//    private float spriteFlipTime = 0.5f;
//    private float spriteFlipTimeLeft = 0.0f;
    float x = 0.0f;
    float y = 0.0f;

    @Override
    public void update(float dt) {
        levelEditorObject.update(dt);
        DebugDraw.addCircle(new Vector2f(x, y), 50, new Vector3f(1, 1,1), 1);
        x += 50f * dt;
        y += 50f * dt;

//        spriteFlipTimeLeft -= dt;
//        if (spriteFlipTimeLeft <= 0) {
//            spriteFlipTimeLeft = spriteFlipTime;
//            spriteIndex++;
//            if(spriteIndex > 3) {
//                spriteIndex = 0;
//            }
//            obj1.getComponent(SpriteRender.class).setSprite(sprites.spriteIndex(spriteIndex));
//        }

        for (GameObject go : this.gObjects) {
            go.update(dt);
        }

        this.renderer.render();
    }

    @Override
    public void imgui() {
        ImGui.begin("Test");

        ImVec2 windowPos = new ImVec2();
        ImGui.getWindowPos(windowPos);
        ImVec2 windowSize = new ImVec2();
        ImGui.getWindowSize(windowSize);

        ImVec2 objectSpace = new ImVec2();
        ImGui.getStyle().getItemSpacing(objectSpace);

        float windowX2 = windowPos.x + windowPos.x;
        for (int i = 0; i < sprites.size(); i++) {
            Sprite sprite = sprites.spriteIndex(i);
            float spriteWidth = sprite.loadWidth() * 3;
            float spriteHeight = sprite.loadHeight() * 3;
            int id = sprite.loadTexId();

            Vector2f[] texCoord = sprite.loadTexCrd();

            ImGui.pushID(i);

            if (ImGui.imageButton(id, spriteWidth, spriteHeight,
                    texCoord[2].x, texCoord[0].y,
                    texCoord[0].x, texCoord[2].y
                )
            ) {
                GameObject obj = Prefab.genSpsObj(sprite, 32, 32);

                // Bind to mouse cursor
                levelEditorObject.getComponent(MouseCtrl.class).pickObj(obj);
            }

            ImGui.popID();

            ImVec2 lastButtonPos = new ImVec2();
            ImGui.getItemRectMax(lastButtonPos);
            float lastButtonX2 = lastButtonPos.x;
            float nextButtonX2 = lastButtonX2 + objectSpace.x + spriteWidth;

            if (i + 1 < sprites.size() && nextButtonX2 < windowX2) {
                ImGui.sameLine();
            }
        }

        ImGui.end();
    }

}

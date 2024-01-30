package scene;

import TCB_Field.*;
import components.*;
import editor.Gizmo;
import editor.WorkViewport;
import imgui.ImGui;
import imgui.ImVec2;
import org.joml.Vector2f;
import utility.AssetsPool;

public class LevelEditorScene extends Scene {
    private SpriteSheet sprites;
    GameObject levelEditorObject = new GameObject("Lvl Editor", new Transform(new Vector2f()), 0);
    public LevelEditorScene() {

    }

    @Override
    public void init() {
        loadRes(); //Don't touch

        sprites = AssetsPool.loadSpSheet("assets/texture/Main char.png");
        SpriteSheet gizmo = AssetsPool.loadSpSheet("assets/texture/Gizmo.png");

        this.viewport = new Viewport(new Vector2f(0, 0)); //View point position

        levelEditorObject.addComponent(new MouseCtrl());
        levelEditorObject.addComponent(new Grid());
        levelEditorObject.addComponent(new WorkViewport(this.viewport));
        levelEditorObject.addComponent(new Gizmo(gizmo.spriteIndex(1), Window.loadImGui().loadProperties()));

        levelEditorObject.start();
    }

    private void loadRes() {
        AssetsPool.loadShader("assets/shaders/default.glsl");

        AssetsPool.addSpSheet("assets/texture/Main char.png",
                new SpriteSheet(AssetsPool.loadTexture("assets/texture/Main char.png"),
                        16, 16, 13, 16)
        );

        AssetsPool.addSpSheet("assets/texture/Gizmo.png",
                new SpriteSheet(AssetsPool.loadTexture("assets/texture/Gizmo.png"),
                         16, 32, 2, 0)
        );

        AssetsPool.loadTexture("assets/texture/Just_a_placeholder.png");

        // Only generate if not existed
        for (GameObject obj : gObjects) {
            if (obj.getComponent(SpriteRender.class) != null) {
                SpriteRender spr = obj.getComponent(SpriteRender.class);
                if (spr.loadTexture() != null) {
                    spr.setTex(AssetsPool.loadTexture(spr.loadTexture().loadFilePath()));
                }
            }
        }
    }

    @Override
    public void update(float dt) {
        levelEditorObject.update(dt);
        this.viewport.adjustProjection();

        for (GameObject go : this.gObjects) {
            go.update(dt);
        }
    }

    @Override
    public void render() {
        this.renderer.render();
    }


    @Override
    public void imgui() {
        ImGui.begin("Level Editor Debug");
        levelEditorObject.imgui();
        ImGui.end();

        ImGui.begin("Sprite list");

        ImVec2 windowPos = new ImVec2();
        ImGui.getWindowPos(windowPos);
        ImVec2 windowSize = new ImVec2();
        ImGui.getWindowSize(windowSize);

        ImVec2 objectSpace = new ImVec2();
        ImGui.getStyle().getItemSpacing(objectSpace);

        float windowX2 = windowPos.x + windowSize.x;
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

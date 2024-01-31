package scene;

import TCB_Field.GameObject;
import TCB_Field.Viewport;
import components.*;
import editor.ImEditorGui;
import editor.WorkViewport;
import imgui.ImGui;
import imgui.ImVec2;
import org.joml.Vector2f;
import utility.AssetsPool;

public class LevelEditorScene extends Scene {
    private SpriteSheet sprites, gizmo;
    GameObject levelEditorObject = this.generateObject("Lvl Editor");
    public LevelEditorScene() {

    }

    @Override
    public void init() {
        loadRes(); //Don't touch

        sprites = AssetsPool.loadSpSheet("assets/texture/Main char.png");
        gizmo = AssetsPool.loadSpSheet("assets/texture/Gizmo.png");

        this.viewport = new Viewport(new Vector2f(0, 0)); //View point position

        levelEditorObject.addComponent(new MouseCtrl());
        levelEditorObject.addComponent(new Grid());
        levelEditorObject.addComponent(new WorkViewport(this.viewport));
        levelEditorObject.addComponent(new GizmoControl(gizmo));
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
                         16, 48, 3, 0)
        );
        AssetsPool.addSpSheet("assets/texture/Just_a_placeholder.png",
                new SpriteSheet(AssetsPool.loadTexture("assets/texture/Just_a_placeholder.png"),
                        64, 64, 1, 0)
        );

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
        String[] spriteSpsA = new String[2];
        spriteSpsA[0] = "assets/texture/Main char.png";
        spriteSpsA[1] = "assets/texture/Just_a_placeholder.png";
        ImEditorGui.drawSpriteList(spriteSpsA, levelEditorObject, windowPos, windowSize);

        ImGui.end();
    }

}

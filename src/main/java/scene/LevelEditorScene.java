package scene;

import TCB_Field.GameObject;
import components.*;
import editor.ImEditorGui;
import editor.WorkViewport;
import imgui.ImGui;
import imgui.ImVec2;
import utility.AssetsPool;

public class LevelEditorScene extends SceneInit {
    private SpriteSheet sprites, gizmo;
    private GameObject levelEditorObject;
    public LevelEditorScene() {

    }

    @Override
    public void init(Scene scene) {

        sprites = AssetsPool.loadSpSheet("assets/texture/Main char.png");
        gizmo = AssetsPool.loadSpSheet("assets/texture/Gizmo.png");
        levelEditorObject = scene.generateObject("Lvl Editor");
        levelEditorObject.isNotSerialize();
        levelEditorObject.addComponent(new MouseCtrl());
        levelEditorObject.addComponent(new Grid());
        levelEditorObject.addComponent(new WorkViewport(scene.viewport()));
        levelEditorObject.addComponent(new GizmoControl(gizmo));

        scene.addObjToScene(levelEditorObject);

    }

    @Override
    public void loadRes(Scene scene) {
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
        for (GameObject obj : scene.loadGameObjects()) {
            if (obj.getComponent(SpriteRender.class) != null) {
                SpriteRender spr = obj.getComponent(SpriteRender.class);
                if (spr.loadTexture() != null) {
                    spr.setTex(AssetsPool.loadTexture(spr.loadTexture().loadFilePath()));
                }
            }
        }
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

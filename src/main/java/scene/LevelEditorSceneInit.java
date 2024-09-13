package scene;

import TCB_Field.GameObject;
import TCB_Field.Prefab;
import components.*;
import editor.ImEditorGui;
import editor.EditorViewport;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiButtonFlags;
import org.joml.Vector2f;
import utility.AssetsPool;

public class LevelEditorSceneInit extends SceneInit {
    private SpriteSheet sprites, gizmo;
    private GameObject levelEditorObject;
    public LevelEditorSceneInit() {

    }

    @Override
    public void init(Scene scene) {

        sprites = AssetsPool.loadSpSheet("assets/texture/Main char.png");
        gizmo = AssetsPool.loadSpSheet("assets/texture/Gizmo.png");

        levelEditorObject = scene.generateObject("Editor");
        levelEditorObject.isNotSerialize();

        levelEditorObject.addComponent(new MouseCtrl());
        levelEditorObject.addComponent(new Grid());
        levelEditorObject.addComponent(new EditorViewport(scene.viewport()));
        levelEditorObject.addComponent(new GizmoControl(gizmo));

        scene.addObjToScene(levelEditorObject);
    }

    @Override
    public void loadResource(Scene scene) {
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
        for (GameObject obj : scene.getGameObject()) {
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
        //String[] spriteSpsA = new String[2];
        //spriteSpsA[0] = "assets/texture/Main char.png";
        //spriteSpsA[1] = "assets/texture/Just_a_placeholder.png";
        //ImEditorGui.drawSpriteList(spriteSpsA, levelEditorObject, windowPos, windowSize);

        ImVec2 objectSpace = new ImVec2();
        ImGui.getStyle().getItemSpacing(objectSpace);

        float windowX2 = windowPos.x + windowSize.x;
        for (int i = 0; i < sprites.size(); i++) {
            Sprite sps = sprites.spriteIndex(i);

            /*
                Ratio in this context is how much smaller (for ratio >= 1) the image is compare to allowed space for object button
                If width < height => height should be scale to match allowed space. (height is currently closer to target)
                    width will use same ratio as height to maintain image aspect ratio.

                Ratio in this context is how much bigger (for ratio < 1) the image is compare to allowed space for object button
                If width < height => width should be scale to match allowed space (width is currently closer to target)
                    height will use same ratio as height to maintain image aspect ratio.

                Keep scaling to minimum for compact design and fast calculation
            */
            //float ratioX = 48.0f / sprites.loadWidth();
            //float ratioY = 48.0f /  sprites.loadWidth();
            //if (ratioX >= 1.0f && ratioY >= 1.0f) {
            //    if (ratioX < ratioY) {
            //        ratioX = ratioY;
            //    } else if (ratioX >= ratioY) {
            //        ratioY = ratioX;
            //    }
            //} else if (ratioX < 1.0f && ratioY < 1.0f) {
            //    if (ratioX >= ratioY) {
            //        ratioX = ratioY;
            //    } else if (ratioX < ratioY) {
            //        ratioY = ratioX;
            //    }
            //}

            //float spriteWidth = sprites.loadWidth() * ratioX;
            //float spriteHeight = sprites.loadHeight() * ratioY;
            float spriteWidth = sps.loadWidth() * 4;
            float spriteHeight = sps.loadHeight() * 4;
            int id = sps.loadTexId();

            Vector2f[] texCoord = sps.loadTexCrd();

            ImGui.pushID(i);


            ImGui.imageButton(id, spriteWidth, spriteHeight,
                    texCoord[2].x, texCoord[0].y,
                    texCoord[0].x, texCoord[2].y
            );

            if (ImGui.isItemClicked()) {
                //float rX = 32.0f / sprites.loadWidth();
                //float rY = 32.0f /  sprites.loadWidth();
                //if (rX >= 1.0f && rY >= 1.0f) {
                //    if (rX < rY) {
                //        rX = rY;
                //    } else if (rX >= rY) {
                //        rY = rX;
                //    }
                //} else if (rX < 1.0f && rY < 1.0f) {
                //    if (rX >= ratioY) {
                //        rX = rY;
                //    } else if (rX < rY) {
                //        rY = rX;
                //    }
                //}
                //GameObject obj = Prefab.genSpsObj(sprites, (sprites.loadWidth() * rX) / 100f, (sprites.loadHeight() * rY) / 100f);

                GameObject obj = Prefab.genSpsObj(sps, 0.32f, 0.32f);

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

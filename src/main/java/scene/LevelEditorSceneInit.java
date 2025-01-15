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
import org.joml.Vector2i;
import utility.AssetsPool;
import utility.TextureScale;

public class LevelEditorSceneInit extends SceneInit {
    private SpriteSheet sprites, gizmo;
    private GameObject levelEditorObject;
    public LevelEditorSceneInit() {

    }

    @Override
    public void init(Scene scene) {

        // TODO: make sprites a list of sprite sheet, load them in using loop. Sprites is determined in scene definition of project file.
        sprites = AssetsPool.loadSpSheet("assets/texture/test objects.png");
        gizmo = AssetsPool.loadSpSheet("assets/texture/Gizmo.png");

        levelEditorObject = scene.generateObject("Editor");
        levelEditorObject.isNotSerialize();

        levelEditorObject.addComponent(new MouseCtrl());
        levelEditorObject.addComponent(new Grid());
        levelEditorObject.addComponent(new EditorViewport(scene.viewport()));
        levelEditorObject.addComponent(new GizmoControl(gizmo));

        scene.addObjToScene(levelEditorObject);
    }

    // TODO: load resource with loop for assets and sheets appeared in scene definition.
    @Override
    public void loadResource(Scene scene) {
        AssetsPool.loadShader("assets/shaders/default.glsl");


        // make a loop to load all defined sheets here.
        AssetsPool.addSpSheet("assets/texture/test objects.png",
                new SpriteSheet(AssetsPool.loadTexture("assets/texture/test objects.png"),
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

        // TODO: add a loop here to start loading all sprites into each category

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

            Vector2f scaledSprite = TextureScale.calculateFitDimension(sps.loadWidth(), sps.loadHeight());

            float spriteWidth = scaledSprite.x;
            float spriteHeight = scaledSprite.y;
            int id = sps.loadTexId();

            Vector2f[] texCoord = sps.loadTexCrd();

            ImGui.pushID(i);


            ImGui.imageButton(id, spriteWidth, spriteHeight,
                    texCoord[2].x, texCoord[0].y,
                    texCoord[0].x, texCoord[2].y
            );

            if (ImGui.isItemClicked()) {

                GameObject obj = Prefab.genSpsObj(sps, sps.loadWidth() / 100f, sps.loadHeight() / 100f);

                // Bind to mouse cursor
                levelEditorObject.getComponent(MouseCtrl.class).pickObj(obj);
            }

            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();

                ImGui.text("Preview");
                ImGui.image(id, spriteWidth * 2, spriteHeight * 2,
                        texCoord[2].x, texCoord[0].y,
                        texCoord[0].x, texCoord[2].y);
                ImGui.text("Width: " + sps.loadWidth());
                ImGui.text("Height: " + sps.loadHeight());

                ImGui.endTooltip();
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

package scene;

import TCB_Field.GameObject;
import TCB_Field.Prefab;
import components.*;
import editor.EditorViewport;
import editor.Project;
import imgui.ImGui;
import imgui.ImVec2;
import org.joml.Vector2f;
import utility.AssetsPool;
import utility.PathResolver;
import utility.TextureScale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static editor.Project.CurrentProject;
import static editor.Project.ProjectRoot;

public class LevelEditorSceneInit extends SceneInit {
    private SpriteSheet gizmo;
    private GameObject levelEditorObject;

    private Map<String, List<SpriteSheet>> categorizedSpriteSheetList = new HashMap<>();
    private List<SpriteSheet> assetList = new ArrayList<>();
    private List<String> sheetKeyList;
    private List<String> assetKeyList;

    private String sceneName = null;
    private Project.projectSceneMap thisScene = null;

    public LevelEditorSceneInit() {}

    /**
     * Initialize LevelEditorScene with a scene name attach to it.
     * This allows loading assigned sheets and assets defined in the project file.
     * @param name a nested key within a scene, pulled from {@link Project}
     */
    public LevelEditorSceneInit(String name) {
        this.sceneName = name;
    }

    @Override
    public void init(Scene scene) {

        if (sceneName != null && CurrentProject != null) {
            thisScene = CurrentProject.getScenes().get(sceneName);

            sheetKeyList = thisScene.getSheet();
            assetKeyList = thisScene.getAsset();

            for (String sheet : sheetKeyList) {
                Project.projectSheetMap sM = CurrentProject.getSheets().get(sheet);

                String category = sM.getCategory();
                String path = PathResolver.resolveRelative(ProjectRoot, sM.getPath());

                SpriteSheet spriteSheet = AssetsPool.loadSpSheet(path);

                categorizedSpriteSheetList.
                        computeIfAbsent(category, key -> new ArrayList<>()).
                        add(spriteSheet);
            }

            for (String asset : assetKeyList) {
                Project.projectAssetMap aM = CurrentProject.getAssets().get(asset);
                String path = PathResolver.resolveRelative(ProjectRoot, aM.getPath());

                assetList.add(AssetsPool.loadSpSheet(path));
            }
        }

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

        if (sceneName != null && CurrentProject != null) {
            thisScene = CurrentProject.getScenes().get(sceneName);

            sheetKeyList = thisScene.getSheet();
            assetKeyList = thisScene.getAsset();

            for (String sheet : sheetKeyList) {
                Project.projectSheetMap sM = CurrentProject.getSheets().get(sheet);
                String absPath = PathResolver.resolveRelative(ProjectRoot, sM.getPath());

                AssetsPool.addSpSheet(absPath,
                        new SpriteSheet(AssetsPool.loadTexture(absPath),
                                sM.getSizeX(), sM.getSizeY(), sM.getCount(), sM.getPadding())
                );
            }

            for (String asset : assetKeyList) {
                Project.projectAssetMap aM = CurrentProject.getAssets().get(asset);
                String absPath = PathResolver.resolveRelative(ProjectRoot, aM.getPath());

                AssetsPool.addSpSheet(absPath,
                        new SpriteSheet(AssetsPool.loadTexture(absPath),
                                aM.getSizeX(), aM.getSizeY(), 1, 0)
                );
            }

        }

        AssetsPool.addSpSheet("assets/texture/Gizmo.png",
                new SpriteSheet(AssetsPool.loadTexture("assets/texture/Gizmo.png"),
                         16, 48, 3, 0)
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

        if (thisScene == null || CurrentProject == null) return;

        ImGui.begin("Sprite list");

        if (ImGui.beginTabBar("SpriteList_TabBar")) {
            for (Map.Entry<String, List<SpriteSheet>> entry : categorizedSpriteSheetList.entrySet()) {
                String category = entry.getKey();
                List<SpriteSheet> sheets = entry.getValue();
                if (ImGui.beginTabItem(category)) {

                    ImVec2 windowPos = new ImVec2();
                    ImGui.getWindowPos(windowPos);
                    ImVec2 windowSize = new ImVec2();
                    ImGui.getWindowSize(windowSize);

                    ImVec2 objectSpace = new ImVec2();
                    ImGui.getStyle().getItemSpacing(objectSpace);

                    float windowX2 = windowPos.x + windowSize.x;

                    for (SpriteSheet sprites : sheets) {
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
                    }

                    ImGui.endTabItem();
                }
            }
            ImGui.endTabBar();
        }
        ImGui.end();
    }

}

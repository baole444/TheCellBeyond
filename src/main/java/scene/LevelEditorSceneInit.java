package scene;

import TCB_Field.GameObject;
import TCB_Field.Prefab;
import TCB_Field.Sound;
import components.*;
import editor.EditorViewport;
import editor.Project;
import imgui.ImGui;
import imgui.ImVec2;
import org.joml.Vector2f;
import utility.AssetsPool;
import utility.PathResolver;
import utility.TextureScale;

import java.io.File;
import java.util.*;

import static editor.ImEditorGui.drawSpriteList;
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
        levelEditorObject.addComponent(new KeyCtrl());
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

        // Temporary
        AssetsPool.addSpSheet("assets/texture/animation_test.png",
                new SpriteSheet(AssetsPool.loadTexture("assets/texture/animation_test.png"),
                        32, 32, 8, 16));

        AssetsPool.addSound("assets/sound/test.ogg", false);

        // Only generate if not existed
        for (GameObject obj : scene.getGameObject()) {
            if (obj.getComponent(SpriteRender.class) != null) {
                SpriteRender spr = obj.getComponent(SpriteRender.class);
                if (spr.loadTexture() != null) {
                    spr.setTex(AssetsPool.loadTexture(spr.loadTexture().loadFilePath()));
                }
            }

            if (obj.getComponent(StateEngine.class) != null) {
                StateEngine stateEngine = obj.getComponent(StateEngine.class);
                stateEngine.reloadTexture();
            }
        }
    }


    @Override
    public void imgui() {
        ImGui.begin("Level Editor Debug");
        levelEditorObject.imgui();
        ImGui.end();
        ImGui.begin("Sprite list");

        if (ImGui.beginTabBar("SpriteList_TabBar")) {

            if (thisScene != null && CurrentProject != null) {
                for (Map.Entry<String, List<SpriteSheet>> entry : categorizedSpriteSheetList.entrySet()) {
                    String category = entry.getKey();
                    List<SpriteSheet> sheets = entry.getValue();
                    if (ImGui.beginTabItem(category)) {

                        ImVec2 windowPos = new ImVec2();
                        ImGui.getWindowPos(windowPos);
                        ImVec2 windowSize = new ImVec2();
                        ImGui.getWindowSize(windowSize);

                        drawSpriteList(sheets, levelEditorObject, windowPos, windowSize);

                        ImGui.endTabItem();
                    }
                }
            }

            if(ImGui.beginTabItem("Prefabrication")) {

                SpriteSheet wheelSprites = AssetsPool.loadSpSheet("assets/texture/animation_test.png");

                Sprite sps = wheelSprites.spriteIndex(0);

                Vector2f scaledSprite = TextureScale.calculateFitDimension(sps.loadWidth(), sps.loadHeight());

                float spriteWidth = scaledSprite.x;
                float spriteHeight = scaledSprite.y;
                int id = sps.loadTexId();

                Vector2f[] texCoord = sps.loadTexCrd();


                ImGui.imageButton(id, spriteWidth, spriteHeight,
                        texCoord[2].x, texCoord[0].y,
                        texCoord[0].x, texCoord[2].y
                );

                if (ImGui.isItemClicked()) {
                    // Testing for now, generate a spinning wheel
                    GameObject obj = Prefab.genWheelSpin();

                    // Bind to mouse cursor
                    levelEditorObject.getComponent(MouseCtrl.class).pickObj(obj);
                }
                ImGui.sameLine();

                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Sound collection")) {
                Collection<Sound> sounds = AssetsPool.loadAllSound();

                for (Sound sound : sounds) {
                    File current = new File(sound.getFilepath());
                    if (ImGui.button(current.getName())) {
                        if (!sound.isPlaying()) {
                            sound.play();
                        } else {
                            sound.stop();
                        }
                    }

                    if (ImGui.getContentRegionAvailX() > 120) {
                        ImGui.sameLine();
                    }
                }

                ImGui.endTabItem();
            }

            ImGui.endTabBar();
        }
        ImGui.end();
    }

}

package scene;

import TheCellBeyond.GameObject;
import components.*;
import editor.EditorViewport;
import editor.project.Project;
import editor.project.ProjectAssetMap;
import editor.project.ProjectSceneMap;
import editor.project.ProjectSheetMap;
import imgui.ImGui;
import imgui.ImVec2;
import utility.AssetsPool;
import utility.PathResolver;
import utility.Settings;

import java.util.*;

import static editor.ImEditorGui.drawSpriteList;
import static editor.project.Project.CurrentProject;
import static editor.project.Project.ProjectRoot;

public class LevelEditorSceneInit extends SceneInit {
    private GameObject levelEditorObject;

    private final Map<String, List<SpriteSheet>> categorizedSpriteSheetList = new HashMap<>();
    private final List<SpriteSheet> assetList = new ArrayList<>();
    private List<String> sheetKeyList;
    private List<String> assetKeyList;

    private String sceneName = null;
    private ProjectSceneMap thisScene = null;

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
        //GameObject testText = Prefab.genText("〄々〆〇ぁあぃいぅツヅテデＢＣＤ仲仳以", "assets/fonts/NotoSansJP.ttf", 24, new Vector4f(1, 1f, 1, 1), GlyphRange.JAPANESE);
        //testText.transform.position.set(1f, 1.5f);
        //scene.addObjToScene(testText);

        if (sceneName != null && CurrentProject != null) {
            thisScene = CurrentProject.getScenes().get(sceneName);

            sheetKeyList = thisScene.getSheet();
            assetKeyList = thisScene.getAsset();

            for (String sheet : sheetKeyList) {
                ProjectSheetMap sM = CurrentProject.getSheets().get(sheet);

                String category = sM.getCategory();
                String path = PathResolver.resolveToAbsolute(ProjectRoot, sM.getPath());

                SpriteSheet spriteSheet = AssetsPool.loadSpriteSheet(path);

                categorizedSpriteSheetList.
                        computeIfAbsent(category, key -> new ArrayList<>()).
                        add(spriteSheet);
            }

            for (String asset : assetKeyList) {
                ProjectAssetMap aM = CurrentProject.getAssets().get(asset);
                String path = PathResolver.resolveToAbsolute(ProjectRoot, aM.getPath());

                assetList.add(AssetsPool.loadSpriteSheet(path));
            }
        }

        SpriteSheet gizmo = AssetsPool.loadSpriteSheet("assets/textures/Gizmo.png");

        levelEditorObject = scene.generateObject("Editor");
        levelEditorObject.setNotSerialize();

        levelEditorObject.addComponent(new MouseCtrl());
        levelEditorObject.addComponent(new KeyCtrl());
        levelEditorObject.addComponent(new Grid());
        levelEditorObject.addComponent(new EditorViewport(scene.viewport()));
        levelEditorObject.addComponent(new GizmoControl(gizmo));

        scene.addObjToScene(levelEditorObject);
    }


    @Override
    public void loadResource(Scene scene) {
        AssetsPool.loadShader(Settings.PATH.DEFAULT_TEXTURE_SHADER);

        if (sceneName != null && CurrentProject != null) {
            thisScene = CurrentProject.getScenes().get(sceneName);

            sheetKeyList = thisScene.getSheet();
            assetKeyList = thisScene.getAsset();

            for (String sheet : sheetKeyList) {
                ProjectSheetMap sM = CurrentProject.getSheets().get(sheet);

                String projectPath = "project://" + sM.getPath();

                AssetsPool.addSpriteSheet(projectPath,
                        new SpriteSheet(AssetsPool.loadTexture(projectPath),
                                sM.getSizeX(), sM.getSizeY(), sM.getCount(), sM.getPadding())
                );
            }

            for (String asset : assetKeyList) {
                ProjectAssetMap aM = CurrentProject.getAssets().get(asset);

                String projectPath = "project://" + aM.getPath();

                AssetsPool.addSpriteSheet(projectPath,
                        new SpriteSheet(AssetsPool.loadTexture(projectPath),
                                aM.getSizeX(), aM.getSizeY(), 1, 0)
                );
            }

        }

        AssetsPool.addSpriteSheet("engine://assets/textures/Gizmo.png",
                new SpriteSheet(AssetsPool.loadTexture("engine://assets/textures/Gizmo.png"),
                         16, 48, 3, 0)
        );

        // Temporary
        AssetsPool.addSpriteSheet("engine://assets/textures/animation_test.png",
                new SpriteSheet(AssetsPool.loadTexture("engine://assets/textures/animation_test.png"),
                        32, 32, 8, 16));

        // Only generate if not existed
        for (GameObject obj : scene.getGameObjects()) {
            if (obj.getComponent(SpriteRenderer.class) != null) {
                SpriteRenderer spr = obj.getComponent(SpriteRenderer.class);
                if (spr.getTexture() != null) {
                    spr.setTexture(AssetsPool.loadTexture(spr.getTexture().getFilePath()));
                }
            }

            if (obj.getComponent(StateEngine.class) != null) {
                StateEngine stateEngine = obj.getComponent(StateEngine.class);
                stateEngine.reloadTexture();
            }

            if (obj.getComponent(TextComponent.class) != null) {
                TextComponent textComponent = obj.getComponent(TextComponent.class);
                textComponent.start();
            }
        }
    }


    @Override
    public void imgui() {
        //ImGui.begin("Level Editor Debug");
        //levelEditorObject.imgui();
        //ImGui.end();
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

            /*
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
             */

            /*
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
             */

            ImGui.endTabBar();
        }
        ImGui.end();
    }

}

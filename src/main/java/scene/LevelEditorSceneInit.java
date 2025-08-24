package scene;

import TheCellBeyond.GameObject;
import TheCellBeyond.Transform;
import components.*;
import editor.EditorViewport;
import editor.project.Project;
import editor.project.ProjectAssetMap;
import editor.project.ProjectSceneMap;
import editor.project.ProjectSheetMap;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import org.joml.Vector2i;
import render.texture.SpriteSheet;
import utility.AssetsPool;
import utility.PathResolver;
import utility.Settings;
import utility.prefabrication.PrefabData;
import utility.prefabrication.PrefabManager;

import java.util.*;

import static editor.project.Project.CurrentProject;
import static editor.project.Project.ProjectRoot;

public class LevelEditorSceneInit extends SceneInit {
    private final Vector2i prefabButtonSize = new Vector2i(200, 30);

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
        if (sceneName != null && CurrentProject != null) {
            thisScene = CurrentProject.scenes().get(sceneName);

            sheetKeyList = thisScene.sheet();
            assetKeyList = thisScene.asset();

            if (sheetKeyList != null) {
                for (String sheet : sheetKeyList) {
                    ProjectSheetMap sM = CurrentProject.sheets().get(sheet);

                    String category = sM.category();
                    String path = PathResolver.resolveToAbsolute(ProjectRoot, sM.path());

                    SpriteSheet spriteSheet = AssetsPool.loadSpriteSheet(path);

                    categorizedSpriteSheetList.
                            computeIfAbsent(category, key -> new ArrayList<>()).
                            add(spriteSheet);
                }
            }

            if (sheetKeyList != null) {
                for (String asset : assetKeyList) {
                    ProjectAssetMap aM = CurrentProject.assets().get(asset);
                    String path = PathResolver.resolveToAbsolute(ProjectRoot, aM.path());

                    assetList.add(AssetsPool.loadSpriteSheet(path));
                }
            }
        }

        SpriteSheet gizmo = AssetsPool.loadSpriteSheet("assets/textures/Gizmo.png");

        levelEditorObject = new GameObject("EditorObject");
        levelEditorObject.setNotSerialize();
        levelEditorObject.addComponent(new Transform());
        levelEditorObject.addComponent(new MouseCtrl());
        levelEditorObject.addComponent(new KeyCtrl());
        levelEditorObject.addComponent(new Grid());
        levelEditorObject.addComponent(new EditorViewport(scene.viewport()));
        levelEditorObject.addComponent(new GizmoControl(gizmo));

        scene.queueForObjectAddition(levelEditorObject);
    }

    @Override
    public void loadResource(Scene scene) {
        AssetsPool.loadShader(Settings.PATH.DEFAULT_TEXTURE_SHADER);

        if (sceneName != null && CurrentProject != null) {
            thisScene = CurrentProject.scenes().get(sceneName);

            sheetKeyList = thisScene.sheet();
            assetKeyList = thisScene.asset();

            if (sheetKeyList != null) {
                for (String sheet : sheetKeyList) {
                    ProjectSheetMap sM = CurrentProject.sheets().get(sheet);

                    String projectPath = "project://" + sM.path();

                    AssetsPool.addSpriteSheet(projectPath,
                            new SpriteSheet(AssetsPool.loadTexture(projectPath),
                                    sM.spriteSizeX(), sM.spriteSizeY(), sM.numberOfSprite(),
                                    sM.spriteSpacingX(), sM.spriteSpacingY(),
                                    sM.spriteStartPosX(), sM.spriteStartPosY())
                    );
                }
            }

            if (assetKeyList != null) {
                for (String asset : assetKeyList) {
                    ProjectAssetMap aM = CurrentProject.assets().get(asset);

                    String projectPath = "project://" + aM.path();

                    AssetsPool.addSpriteSheet(projectPath,
                            new SpriteSheet(AssetsPool.loadTexture(projectPath),
                                    aM.sizeX(), aM.sizeY(), 1, 0)
                    );
                }
            }
        }

        AssetsPool.addSpriteSheet("engine://assets/textures/Gizmo.png",
                new SpriteSheet(AssetsPool.loadTexture("engine://assets/textures/Gizmo.png"),
                         16, 48, 3, 0)
        );

        // Only generate if not existed
        for (GameObject obj : scene.getGameObjects().values()) {
            if (obj.getFirstComponent(SpriteRenderer.class) != null) {
                List<SpriteRenderer> sps = obj.getComponents(SpriteRenderer.class);
                for (SpriteRenderer sprite : sps) {
                    if (sprite.getTexture() != null) {
                        sprite.setTexture(AssetsPool.loadTexture(sprite.getTexture().getFilePath()));
                    }
                }
            }

            if (obj.getFirstComponent(StateEngine.class) != null) {
                List<StateEngine> states = obj.getComponents(StateEngine.class);
                for (StateEngine state : states) {
                    state.reloadTexture();
                }
            }

            if (obj.getFirstComponent(TextRenderer.class) != null) {
                List<TextRenderer> texts = obj.getComponents(TextRenderer.class);
                for (TextRenderer txt : texts) {
                    txt.start();
                }
            }
        }
    }


    @Override
    public void imgui() {
        //ImGui.begin("Level Editor Debug");
        //levelEditorObject.imgui();
        //ImGui.end();
        ImGui.begin("Resources");

        if (ImGui.beginTabBar("Resource_TabBar")) {

            // TODO: we no longer generate object with sprite, this should be repurpose to showing added resource like sprites in the spritesheet
            /*
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

             */

            if (ImGui.beginTabItem("Prefabrication")) {
                drawPrefabList();
                ImGui.endTabItem();
            }

            ImGui.endTabBar();
        }
        ImGui.end();
    }

    private void drawPrefabList() {
        PrefabManager manager = PrefabManager.get();
        if (manager.getPrefabNames().isEmpty()) {
            manager.loadAllPrefabs();
        }

        List<String> prefabNames = manager.getPrefabNames();

        if (prefabNames.isEmpty()) {
            ImGui.text("No object prefabs available");
            ImGui.text("To add an Object as a prefabrication blueprint");
            ImGui.text("Right click on an Object in the Scene tree window and select \"Save as Prefab\"");
            return;
        }

        for (String name : prefabNames) {
            PrefabData data = manager.getPrefabData(name);

            if (ImGui.button(name, prefabButtonSize.x, prefabButtonSize.y)) {
                GameObject instance = manager.instantiatePrefab(name);
                if (name != null) {
                    levelEditorObject.getFirstComponent(MouseCtrl.class).pickObj(instance);
                }
            }

            if (ImGui.isItemHovered() && data != null) {
                ImGui.beginTooltip();
                ImGui.text("Prefab: " + name);
                ImGui.text(data.description());
                ImGui.text("Click to instantiate");
                ImGui.endTooltip();
            }

            ImGui.sameLine();

            ImGui.pushID(name);
            ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);

            if (ImGui.button("X", Math.round((float) prefabButtonSize.x / 4), prefabButtonSize.y)) {
                if (manager.deletePrefab(name)) {
                    System.out.println("Deleted prefab: " + name);
                }
            }

            ImGui.popStyleColor(3);
            ImGui.popID();

            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.text("Delete this prefab blueprint");
                ImGui.endTooltip();
            }

            ImGui.newLine();
        }
    }

}

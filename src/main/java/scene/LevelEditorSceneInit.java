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
import imgui.flag.ImGuiCol;
import org.joml.Vector2i;
import render.texture.SpriteSheet;
import utility.AssetsPool;
import utility.PathResolver;
import utility.Settings;
import utility.prefabrication.PrefabData;
import utility.prefabrication.PrefabManager;

import java.util.*;

import static editor.ImEditorGui.drawSpriteList;
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

        scene.queueForObjectAddition(levelEditorObject);
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

            if (obj.getFirstComponent(TextComponent.class) != null) {
                List<TextComponent> texts = obj.getComponents(TextComponent.class);
                for (TextComponent txt : texts) {
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

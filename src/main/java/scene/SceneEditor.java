package scene;

import TheCellBeyond.GameObject;
import TheCellBeyond.Transform;
import TheCellBeyond.Window;
import components.*;
import editor.EditorSceneCtrl;
import editor.dialog.AddSpriteSheetDialog;
import editor.payload.SpriteDragDropPayload;
import editor.project.Project;
import editor.project.ProjectData;
import editor.project.ProjectSheetMap;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTabItemFlags;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.texture.Sprite;
import render.texture.SpriteSheet;
import utility.AssetsPool;
import utility.PathResolver;
import utility.Settings;
import utility.TextureScale;
import utility.prefabrication.PrefabData;
import utility.prefabrication.PrefabManager;

import java.util.*;
import java.util.stream.Collectors;

public class SceneEditor extends SceneInit {
    private final Vector2i prefabButtonSize = new Vector2i(200, 30);

    private GameObject levelEditorObject;

    private final Map<String, Map<String, SpriteSheet>> categorizedSpriteSheetList = new HashMap<>();
    private final List<SpriteSheet> assetList = new ArrayList<>();

    private String sceneName = null;
    private transient final ImString spriteSearchFilter;

    public SceneEditor() {
        this("New scene");
    }

    /**
     * Initialize LevelEditorScene with a scene name attach to it.
     * @param name a nested key within a scene, pulled from {@link Project}
     */
    public SceneEditor(String name) {
        this.sceneName = name;
        spriteSearchFilter = new ImString(128);
    }

    @Override
    public void init(Scene scene) {
        loadCategorizedSheet();

        SpriteSheet gizmo = AssetsPool.loadSpriteSheet("assets/textures/Gizmo.png");
        levelEditorObject = new GameObject("EditorObject");
        levelEditorObject.setNotSerialize();
        levelEditorObject.addComponent(new Transform());
        levelEditorObject.addComponent(new MouseCtrl());
        levelEditorObject.addComponent(new KeyCtrl());
        levelEditorObject.addComponent(new Grid());
        levelEditorObject.addComponent(new EditorSceneCtrl(scene.viewport()));
        levelEditorObject.addComponent(new GizmoControl(gizmo));

        scene.queueForObjectAddition(levelEditorObject);
    }

    @Override
    public void loadResource(Scene scene) {
        Project.loadProjectData();
        AssetsPool.loadShader(Settings.PATH.DEFAULT_TEXTURE_SHADER);
        AssetsPool.addSpriteSheet("engine://assets/textures/Gizmo.png",
                new SpriteSheet(AssetsPool.loadTexture("engine://assets/textures/Gizmo.png"),
                         16, 48, 3, 0)
        );

        loadGameObjectFromScene(scene);
    }

    public void reloadResource() {
        Project.loadProjectData();
        loadCategorizedSheet();
        loadGameObjectFromScene(Window.getScene()); // Why ? because you can only reload the scene you are currently on
    }

    @Override
    public void imgui() {
        ImGui.begin("Resources");
        if (ImGui.beginTabBar("Resource_TabBar")) {
            ImGui.pushStyleColor(ImGuiCol.Tab, 0.6f, 0.25f, 0.0f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.TabHovered, 0.75f, 0.31f, 0.0f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.TabActive, 0.6f, 0.25f, 0.0f, 1.0f);
            ImGui.pushItemWidth(60);
            if (ImGui.tabItemButton("Reload", ImGuiTabItemFlags.Leading)) {
                reloadResource();
                System.out.println("Resource reloaded");
            }
            ImGui.popStyleColor(3);
            ImGui.popItemWidth();

            if (ImGui.beginTabItem("Sprite Sheets")) {
                ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.8f, 0.3f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.7f, 0.2f, 1.0f);
                if (ImGui.button("Add", 60, 0)) AddSpriteSheetDialog.show(false);
                ImGui.popStyleColor(3);
                ImGui.sameLine();
                ImGui.text("    "); ImGui.sameLine();
                ImGui.text("Search sheet:"); ImGui.sameLine();
                ImGui.pushItemWidth(360);
                ImGui.inputTextWithHint("##Search", "Enter name of a sheet...", spriteSearchFilter);
                ImGui.popItemWidth();
                ImGui.sameLine();
                if (ImGui.button("Clear search")) spriteSearchFilter.clear();
                drawSpriteList();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Prefabrication")) {
                drawPrefabList();
                ImGui.endTabItem();
            }

            ImGui.endTabBar();
        }

        AddSpriteSheetDialog.imgui();

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

    private void drawSpriteList() {
        if (categorizedSpriteSheetList.isEmpty()) {
            ImGui.textDisabled("No sprite sheet added, click \"Add\" to import new sheet");
            return;
        }

        Map<String, Map<String, SpriteSheet>> filterCategorizedSheetMap = searchFilter(spriteSearchFilter.get(), categorizedSpriteSheetList);
        if (filterCategorizedSheetMap.isEmpty()) {
            ImGui.textDisabled("No sheet contains \"" + spriteSearchFilter.get() + "\" in their name");
            return;
        }
        if (ImGui.beginTabBar("Sheet_category_tabBar")) {
            for (Map.Entry<String, Map<String, SpriteSheet>> categoryEntry : filterCategorizedSheetMap.entrySet()) {
                String category = categoryEntry.getKey();
                Map<String, SpriteSheet> sheetMap = categoryEntry.getValue();

                if (ImGui.beginTabItem(category)) {
                    for (Map.Entry<String, SpriteSheet> sheetEntry : sheetMap.entrySet()) {
                        String sheetName = sheetEntry.getKey();
                        SpriteSheet sheet = sheetEntry.getValue();

                        if (ImGui.collapsingHeader(sheetName)) {
                            drawSpriteItems(sheetName, sheet);
                            ImGui.newLine();
                        }
                    }

                    ImGui.endTabItem();
                }
            }

            ImGui.endTabBar();
        }
    }

    private void drawSpriteItems (String name, SpriteSheet sheet) {
        float spacing = ImGui.getStyle().getItemSpacingX();
        float availWidth = ImGui.getContentRegionAvailX();
        float consumedWidth = 0.0f;

        for (int i = 0; i < sheet.numberOfAvailableSprites(); i++) {
            Sprite sprite = sheet.spriteIndex(i);
            int textureID = sprite.getTextureID();
            Vector2f scaledSpriteSize = TextureScale.calculateFitDimension(sprite.getWidth(), sprite.getHeight(), 64, 64);
            Vector2f[] textureCoordinates = sprite.getTextureCoordinates();

            String compositeId = name + i;
            ImGui.pushID(compositeId);

            ImGui.imageButton(compositeId, textureID, scaledSpriteSize.x, scaledSpriteSize.y,
                    textureCoordinates[2].x, textureCoordinates[0].y,
                    textureCoordinates[0].x, textureCoordinates[2].y
            );
            float buttonWidth = ImGui.getItemRectSizeX() + spacing;
            consumedWidth += buttonWidth;

            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.text("Preview");
                Vector2f previewImageSize = TextureScale.calculateFitDimension(sprite.getWidth(), sprite.getHeight(), 160.0f, 160.f);
                ImGui.image(textureID, previewImageSize.x, previewImageSize.y,
                        textureCoordinates[2].x, textureCoordinates[0].y,
                        textureCoordinates[0].x, textureCoordinates[2].y);
                ImGui.text("Width: " + sprite.getWidth());
                ImGui.text("Height: " + sprite.getHeight());
                ImGui.endTooltip();
            }

            if (ImGui.beginDragDropSource()) {
                SpriteDragDropPayload.setPayload(sprite);
                ImGui.setDragDropPayload(SpriteDragDropPayload.getPayloadType(), sprite);

                ImGui.text("Sheet: " + sprite.getTexture().getFilePath());
                ImGui.text("Index: " + i);
                Vector2f previewImageSize = TextureScale.calculateFitDimension(sprite.getWidth(), sprite.getHeight(), 80.0f, 80.f);
                ImGui.image(textureID, previewImageSize.x, previewImageSize.y,
                        textureCoordinates[2].x, textureCoordinates[0].y,
                        textureCoordinates[0].x, textureCoordinates[2].y);

                ImGui.endDragDropSource();
            }

            ImGui.popID();
            if (consumedWidth + buttonWidth <= availWidth) {
                ImGui.sameLine();
                continue;
            }

            consumedWidth = 0.0f;
        }
    }


    private Map<String, Map<String, SpriteSheet>> searchFilter(String filterTerm, Map<String, Map<String, SpriteSheet>> categorizedSheets) {
        if (filterTerm == null || filterTerm.isBlank()) {
            return new HashMap<>(categorizedSheets);
        }

        String sanctioned = filterTerm.trim().toLowerCase();

        // Basically this filter the sheet map with matching filter term, then filter out the tab map where there is no sheet map inside.
        return categorizedSheets.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, tab -> tab.getValue().entrySet().stream()
                        .filter(header -> header.getKey().toLowerCase().contains(sanctioned))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                ))
                .entrySet().stream().filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void loadCategorizedSheet() {
        ProjectData project = Project.currentProject();

        if (project != null && project.sheets() != null) {
            for (Map.Entry<String, Map<String, ProjectSheetMap>> categories : project.sheets().entrySet()) {
                String category = categories.getKey();
                Map<String, SpriteSheet> categorySheets = new HashMap<>();

                for (Map.Entry<String, ProjectSheetMap> sheets : categories.getValue().entrySet()) {
                    String name = sheets.getKey();
                    ProjectSheetMap sM = sheets.getValue();
                    String path = PathResolver.resolveToAbsolute(Project.projectRoot(), sM.path());

                    SpriteSheet spriteSheet = AssetsPool.loadSpriteSheet(path);
                    if (spriteSheet != null) categorySheets.put(name, spriteSheet);
                }

                if (!categorySheets.isEmpty()) categorizedSpriteSheetList.put(category, categorySheets);
            }
        }
    }

    private void loadGameObjectFromScene(Scene scene) {
        for (GameObject obj : scene.getGameObjects().values()) {
            for (SpriteRenderer sprite : obj.getComponents(SpriteRenderer.class)) {
                if (sprite.getTexture() != null) {
                    sprite.setTexture(AssetsPool.loadTexture(sprite.getTexture().getFilePath()));
                }
            }

            for (TextRenderer txt : obj.getComponents(TextRenderer.class)) {
                txt.start();
            }
        }
    }
}

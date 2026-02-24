package scene;

import TheCellBeyond.GameObject;
import TheCellBeyond.Transform2D;
import components.*;
import editor.EditorIcons;
import editor.components.*;
import editor.EditorWidget;
import editor.dialog.AddSpriteSheetDialog;
import editor.dialog.AddTextureUnitDialog;
import editor.payload.SpriteDragDropPayload;
import eventviewer.event.Event;
import project.Project;
import project.ProjectAssetMap;
import project.ProjectData;
import project.ProjectSheetMap;
import eventviewer.EngineEventCallback;
import eventviewer.EngineEventListener;
import eventviewer.event.EditorEvent;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTabItemFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.texture.Sprite;
import render.texture.SpriteSheet;
import render.texture.TextureUnit;
import utility.AssetsPool;
import utility.UnifiedPaths;
import utility.TextureScale;
import utility.prefabrication.PrefabData;
import utility.prefabrication.PrefabManager;

import java.util.*;
import java.util.stream.Collectors;

public class SceneEditor extends SceneLoader implements EngineEventListener {
    public static final String WINDOW_ID = "Resources###Editor_Project_Resource";
    private final Vector2i prefabButtonSize = new Vector2i(200, 30);
    private GameObject levelEditorObject;
    private final Map<String, Map<String, SpriteSheet>> categorizedSpriteSheetList = new HashMap<>();
    private final Map<String, Map<String, SpriteSheet>> filteredSpriteSheetList = new HashMap<>();
    private final Map<UUID, TextureUnit> textureUnits = new HashMap<>();
    private transient final ImString spriteSearchFilter;
    private transient boolean filterChanged = true;

    public SceneEditor() {
        spriteSearchFilter = new ImString(128);
        EngineEventCallback.register(this);
    }

    @Override
    public void onSceneStarted(Scene scene) {}

    @Override
    public void loadResource(Scene scene) {
        Project.loadProjectData();
        loadCategorizedSheet();
        loadTextureUnits();

        levelEditorObject = new GameObject("EditorObject");
        levelEditorObject.setNotSerialize();
        levelEditorObject.addComponents(new IsNotSelectable(), new Transform2D(),
                new EditorMouseCtrl(), new EditorKeyCtrl(), new EditorGrid(),
                new EditorTileMapGrid(), new EditorTileMapCtrl(),
                new EditorSceneCtrl(scene.viewport()), new EditorGizmoCtrl()
        );
        scene.queueForObjectAddition(levelEditorObject, null);
    }

    public void reloadResource() {
        Project.loadProjectData();
        loadCategorizedSheet();
        loadTextureUnits();
        filterChanged = true;
    }

    @Override
    public void imgui() {
        if (!ImGui.begin(WINDOW_ID, ImGuiWindowFlags.NoCollapse)) {
            ImGui.end();
            return;
        }
        ImGui.text("Resources");
        ImGui.separator();
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
                if (ImGui.button("Add", 60.0f, 0.0f)) AddSpriteSheetDialog.show(false);
                ImGui.popStyleColor(3);
                ImGui.sameLine();
                ImGui.text("Search:"); ImGui.sameLine();
                float buttonW = 30.0f;
                float searchW = Math.min(360.0f, ImGui.getContentRegionAvailX() - buttonW - ImGui.getStyle().getItemSpacingX());
                ImGui.pushItemWidth(searchW);
                if (ImGui.inputTextWithHint("##SE_SpriteSheetSearch", "Enter sheet name...", spriteSearchFilter)) filterChanged = true;
                ImGui.popItemWidth();
                ImGui.sameLine();
                ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
                if (ImGui.button("X", buttonW, 0.0f)) {
                    spriteSearchFilter.clear();
                    filterChanged = true;
                }
                ImGui.popStyleColor(3);
                drawSpriteList();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Assets")) {
                ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.8f, 0.3f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.7f, 0.2f, 1.0f);
                if (ImGui.button("Add", ImGui.getContentRegionAvailX(), 0.0f)) AddTextureUnitDialog.show();
                ImGui.popStyleColor(3);
                drawAssetsTextureUnit();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Prefabrication")) {
                drawPrefabList();
                ImGui.endTabItem();
            }

            ImGui.endTabBar();
        }

        AddSpriteSheetDialog.imgui();
        AddTextureUnitDialog.imgui();

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

            if (ImGui.button(name, prefabButtonSize.x, 0.0f)) {
                GameObject instance = manager.instantiatePrefab(name);
                if (instance != null) {
                    levelEditorObject.getFirstComponent(EditorMouseCtrl.class).pickObj(instance);
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
            boolean delete = EditorWidget.iconButton("##delete_prefab_" + name, EditorIcons.Icons.Delete, "Delete '" + name + "' prefab blueprint");
            if (delete) manager.deletePrefab(name);

            ImGui.newLine();
        }
    }

    private void drawSpriteList() {
        if (categorizedSpriteSheetList.isEmpty()) {
            ImGui.beginDisabled();
            ImGui.textWrapped("No sprite sheet added, click \"Add\" to import new sheet");
            ImGui.endDisabled();
            return;
        }

        searchFilter(spriteSearchFilter.get(), categorizedSpriteSheetList);
        if (filteredSpriteSheetList.isEmpty()) {
            ImGui.beginDisabled();
            ImGui.textWrapped("No sheet contains \"" + spriteSearchFilter.get() + "\" in their name");
            ImGui.endDisabled();
            return;
        }
        if (ImGui.beginTabBar("Sheet_category_tabBar")) {
            for (Map.Entry<String, Map<String, SpriteSheet>> categoryEntry : filteredSpriteSheetList.entrySet()) {
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

    private void drawAssetsTextureUnit() {
        if (textureUnits.isEmpty()) {
            ImGui.beginDisabled();
            ImGui.textWrapped("No texture unit added, click \"Add\" to import new unit");
            ImGui.endDisabled();
            return;
        }

        float spacing = ImGui.getStyle().getItemSpacingX();
        float availWidth = ImGui.getContentRegionAvailX();
        float consumedWidth = 0.0f;

        for (Map.Entry<UUID, TextureUnit> unit : textureUnits.entrySet()) {
            Sprite sprite = unit.getValue().getSprite();
            int textureID = sprite.getTextureID();
            Vector2f scaledSpriteSize = TextureScale.calculateFitDimension(sprite.getWidth(), sprite.getHeight(), 32, 32);
            Vector2f[] textureCoordinates = sprite.getTextureCoordinates();

            String compositeId = unit.getKey().toString() + "_TU";
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

                ImGui.text("Texture: " + sprite.getTexture().getCanonicalPath());
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

    private void drawSpriteItems (String name, SpriteSheet sheet) {
        float spacing = ImGui.getStyle().getItemSpacingX();
        float availWidth = ImGui.getContentRegionAvailX();
        float consumedWidth = 0.0f;

        for (int i = 0; i < sheet.numberOfAvailableSprites(); i++) {
            Sprite sprite = sheet.spriteIndex(i);
            int textureID = sprite.getTextureID();
            Vector2f scaledSpriteSize = TextureScale.calculateFitDimension(sprite.getWidth(), sprite.getHeight(), 32, 32);
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

                ImGui.text("Sheet: " + sprite.getTexture().getCanonicalPath());
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

    private void searchFilter(String filterTerm, Map<String, Map<String, SpriteSheet>> categorizedSheets) {
        if (filterTerm == null || !filterChanged) return;

        String sanctioned = filterTerm.trim().toLowerCase();
        filteredSpriteSheetList.clear();
        if (sanctioned.isEmpty()) {
            filteredSpriteSheetList.putAll(categorizedSheets);
            filterChanged = false;
            return;
        }

        // Basically this filter the sheet map with matching filter term, then filter out the tab map where there is no sheet map inside.
        filteredSpriteSheetList.putAll(categorizedSheets.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, tab -> tab.getValue().entrySet().stream()
                        .filter(header -> header.getKey().toLowerCase().contains(sanctioned))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                ))
                .entrySet().stream().filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

        filterChanged = false;
    }

    private void loadCategorizedSheet() {
        ProjectData project = Project.currentProject();

        if (project == null || project.sheets() == null) return;
        categorizedSpriteSheetList.clear();

        for (Map.Entry<String, Map<String, ProjectSheetMap>> categories : project.sheets().entrySet()) {
            String category = categories.getKey();
            Map<String, SpriteSheet> categorySheets = new HashMap<>();

            for (Map.Entry<String, ProjectSheetMap> sheets : categories.getValue().entrySet()) {
                String name = sheets.getKey();
                ProjectSheetMap sM = sheets.getValue();
                String path = UnifiedPaths.resolveToAbsolute(Project.projectRoot(), sM.path());

                SpriteSheet spriteSheet = AssetsPool.getSpriteSheet(path);
                if (spriteSheet != null) categorySheets.put(name, spriteSheet);
            }

            if (!categorySheets.isEmpty()) categorizedSpriteSheetList.put(category, categorySheets);
        }
    }

    private void loadTextureUnits() {
        ProjectData project = Project.currentProject();

        if (project == null || project.assets() == null) return;
        textureUnits.clear();

        for (Map.Entry<UUID, ProjectAssetMap> entry : project.assets().entrySet()) {
            UUID uuid = entry.getKey();
            String path = entry.getValue().path();
            if (path == null || path.isEmpty()) continue;
            TextureUnit unit = AssetsPool.getTextureUnit(path);

            if (unit != null) textureUnits.put(uuid, unit);
        }
    }

    @Override
    public void onSceneEnd() {
        dispose();
    }

    @Override
    public void onEventEmit(Object object, Event event) {
        if (!(event instanceof EditorEvent editorEvent)) return;
        if (editorEvent.type == EditorEvent.Type.ReloadSceneResource) reloadResource();
    }
}

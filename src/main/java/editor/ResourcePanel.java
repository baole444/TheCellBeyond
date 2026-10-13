package editor;

import TheCellBeyond.GameObject;
import editor.dialogs.AddSingleSpriteDialog;
import editor.dialogs.AddSpriteSheetDialog;
import editor.payload.SpriteDragDropPayload;
import imgui.ImGui;
import imgui.flag.*;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector2i;
import project.Project;
import project.ProjectAsset;
import project.ProjectData;
import project.ProjectSheet;
import render.texture.SingleSprite;
import render.texture.Sprite;
import render.texture.SpriteSheet;
import scene.SceneEditor;
import utility.AssetManager;
import utility.TextureScale;
import utility.UnifiedPaths;
import utility.prefabrication.PrefabData;
import utility.prefabrication.PrefabManager;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class ResourcePanel {
    static final String WindowID = "Resources###Editor_Project_Resource";
    private static final Vector2i prefabButtonSize = new Vector2i(200, 30);
    private static final Map<String, Map<String, SpriteSheet>> categorizedSpriteSheetList = new HashMap<>();
    private static final Map<String, Map<String, SpriteSheet>> filteredSpriteSheetList = new HashMap<>();
    private static final Map<ProjectAsset, SingleSprite> singleSprites = new LinkedHashMap<>();
    private static final ImString spriteSearchFilter = new ImString(256);
    private static boolean filterChanged = true;
    private static boolean removedSkip = false;

    static void refreshCache() {
        loadCategorizedSheet();
        loadSingleSprites();
        filterChanged = true;
    }

    static void imgui() {
        if (!ImGui.begin(WindowID, ImGuiWindowFlags.NoCollapse)) {
            ImGui.end();
            return;
        }
        if (removedSkip) refreshCache();
        ImGui.text("Resources");
        ImGui.separator();
        if (ImGui.beginTabBar("Resource_TabBar")) {
            ImGui.pushStyleColor(ImGuiCol.Tab, 0.6f, 0.25f, 0.0f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.TabHovered, 0.75f, 0.31f, 0.0f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.TabActive, 0.6f, 0.25f, 0.0f, 1.0f);
            ImGui.pushItemWidth(60);
            if (ImGui.tabItemButton("Reload", ImGuiTabItemFlags.Leading)) {
                refreshCache();
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
                if (ImGui.button("Add", ImGui.getContentRegionAvailX(), 0.0f)) AddSingleSpriteDialog.show();
                ImGui.popStyleColor(3);
                drawAssetsSingleSprite();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Prefabrication")) {
                drawPrefabList();
                ImGui.endTabItem();
            }
            ImGui.endTabBar();
        }
        AddSpriteSheetDialog.imgui();
        AddSingleSpriteDialog.imgui();
        ImGui.end();
    }

    private static void drawPrefabList() {
        List<String> prefabNames = PrefabManager.getPrefabNames();
        if (prefabNames.isEmpty()) {
            ImGui.text("No object prefabs available");
            ImGui.text("To add an Object as a prefabrication blueprint");
            ImGui.text("Right click on an Object in the Scene tree window and select \"Save as Prefab\"");
            return;
        }
        for (String name : prefabNames) {
            PrefabData data = PrefabManager.getPrefabData(name);
            if (ImGui.button(name, prefabButtonSize.x, 0.0f)) {
                GameObject instance = PrefabManager.instantiatePrefab(name);
                SceneEditor.pickUpObject(instance);
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
            if (delete) PrefabManager.deletePrefab(name);
            ImGui.newLine();
        }
    }

    private static void drawSpriteList() {
        if (categorizedSpriteSheetList.isEmpty()) {
            ImGui.beginDisabled();
            ImGui.textWrapped("No sprite sheet added, click \"Add\" to import new sheet");
            ImGui.endDisabled();
            return;
        }
        searchFilter(spriteSearchFilter.get());
        if (filteredSpriteSheetList.isEmpty()) {
            ImGui.beginDisabled();
            ImGui.textWrapped("No sheet contains \"" + spriteSearchFilter.get() + "\" in their name");
            ImGui.endDisabled();
            return;
        }
        if (!ImGui.beginTabBar("Sheet_category_tabBar")) return;
        for (Map.Entry<String, Map<String, SpriteSheet>> categoryEntry : filteredSpriteSheetList.entrySet()) {
            if (removedSkip) break;
            String category = categoryEntry.getKey();
            Map<String, SpriteSheet> sheetMap = categoryEntry.getValue();
            if (!ImGui.beginTabItem(category)) continue;
            if (!ImGui.beginTable("##SE_SpriteSheet_Content_Headers_Layout", 2, ImGuiTableFlags.SizingFixedFit)) {
                ImGui.endTabItem();
                continue;
            }
            ImGui.tableSetupColumn("##SE_SpriteSheet_Content_Preview_Column", ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableSetupColumn("##SE_SpriteSheet_Content_Remove_Column", ImGuiTableColumnFlags.WidthFixed);
            for (Map.Entry<String, SpriteSheet> sheetEntry : sheetMap.entrySet()) {
                if (removedSkip) break;
                String sheetName = sheetEntry.getKey();
                SpriteSheet sheet = sheetEntry.getValue();
                ImGui.tableNextColumn();
                boolean openSheet = ImGui.collapsingHeader(sheetName + "##SE_SpriteSheet_" + category + "_" + sheetName + "_Sprites_List_Header");
                ImGui.tableNextColumn();
                boolean removed = EditorWidget.iconButton(String.format("Delete##SE_Delete_SpriteSheet_%s_%s", category, sheetName), EditorIcons.Icons.Delete, String.format("Remove %s sheet import (does not remove image on disk)", sheetName));
                if (removed) {
                    Project.removeSheet(category, sheetName);
                    removedSkip = true;
                }
                ImGui.tableNextColumn();
                if (openSheet && !removed) drawSpriteItems(sheetName, sheet);
                ImGui.tableNextColumn();
            }
            ImGui.endTable();
            ImGui.endTabItem();
        }
        ImGui.endTabBar();
    }

    private static void drawAssetsSingleSprite() {
        if (singleSprites.isEmpty()) {
            ImGui.beginDisabled();
            ImGui.textWrapped("No single sprite added, click \"Add\" to import new sprite");
            ImGui.endDisabled();
            return;
        }
        if (!ImGui.beginTable("##SE_Asset_Content_Layout", 3, ImGuiTableFlags.SizingFixedFit)) return;
        ImGui.tableSetupColumn("#SE_Asset_Content_Preview_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("SE_Asset_Content_Name_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("SE_Asset_Content_Delete_Column", ImGuiTableColumnFlags.WidthFixed);
        for (Map.Entry<ProjectAsset, SingleSprite> unit : singleSprites.entrySet()) {
            if (removedSkip) break;
            ImGui.tableNextColumn();
            Sprite sprite = unit.getValue().sprite();
            if (!sprite.hasTexture()) {
                ImGui.tableNextColumn();
                ImGui.tableNextColumn();
                continue;
            }
            ProjectAsset asset = unit.getKey();
            EditorWidget.iconButton(String.format("%s/%s_SS", asset.category(), asset.name()), sprite, null, 32.0f, 32.0f);
            float height = ImGui.getItemRectSizeY();
            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.text("Preview");
                int textureID = sprite.textureID();
                Vector2f[] textureCoordinates = sprite.textureCoordinates();
                Vector2f previewImageSize = TextureScale.calculateFitDimension(sprite.width(), sprite.height(), 160.0f, 160.f);
                ImGui.image(textureID, previewImageSize.x, previewImageSize.y,
                        textureCoordinates[2].x, textureCoordinates[0].y,
                        textureCoordinates[0].x, textureCoordinates[2].y);
                ImGui.endTooltip();
            }
            if (ImGui.beginDragDropSource()) {
                SpriteDragDropPayload.setPayload(sprite);
                ImGui.setDragDropPayload(SpriteDragDropPayload.getPayloadType(), sprite);
                ImGui.text("Texture: " + sprite.textureReference());
                Vector2f previewImageSize = TextureScale.calculateFitDimension(sprite.width(), sprite.height(), 80.0f, 80.f);
                int textureID = sprite.textureID();
                Vector2f[] textureCoordinates = sprite.textureCoordinates();
                ImGui.image(textureID, previewImageSize.x, previewImageSize.y,
                        textureCoordinates[2].x, textureCoordinates[0].y,
                        textureCoordinates[0].x, textureCoordinates[2].y);
                ImGui.endDragDropSource();
            }
            ImGui.tableNextColumn();
            ImGui.setCursorPosY(ImGui.getCursorPosY() + (height - ImGui.getTextLineHeight()) / 2.0f);
            String path = asset.path();
            int lastSeparatorIndex = path.lastIndexOf('/');
            if (lastSeparatorIndex + 1 <= path.length()) path = path.substring(lastSeparatorIndex + 1);
            path = UnifiedPaths.stripMetadata(path);
            String infoText = String.format("%s (%d, %d)%n%s", asset.name(), asset.sizeX(), asset.sizeY(), path);
            ImGui.text(infoText);
            if (ImGui.isItemHovered() && ImGui.calcTextSizeX(infoText) >= ImGui.getContentRegionAvailX()) {
                ImGui.beginTooltip();
                ImGui.text(path);
                ImGui.endTooltip();
            }
            ImGui.tableNextColumn();
            ImGui.setCursorPosY(ImGui.getCursorPosY() + (height - ImGui.getTextLineHeightWithSpacing()) / 2.0f);
            if (!EditorWidget.iconButton(String.format("Delete##RP_Delete_%s_%s_SingleSprite", asset.category(), asset.name()), EditorIcons.Icons.Delete, String.format("Remove single sprite '%s' import (does not remove image on disk)", asset.name()))) continue;
            Project.removeAsset(asset.category(), asset.name());
            removedSkip = true;
            ImGui.spacing();
        }
        ImGui.endTable();
    }

    private static void drawSpriteItems (String name, SpriteSheet sheet) {
        float spacing = ImGui.getStyle().getItemSpacingX();
        float availWidth = ImGui.getContentRegionAvailX();
        float consumedWidth = 0.0f;
        for (int i = 0; i < sheet.availableSpriteCount(); i++) {
            Sprite sprite = sheet.spriteIndex(i);
            if (sprite == null) continue;
            int textureID = sprite.textureID();
            Vector2f scaledSpriteSize = TextureScale.calculateFitDimension(sprite.width(), sprite.height(), 32, 32);
            Vector2f[] textureCoordinates = sprite.textureCoordinates();
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
                Vector2f previewImageSize = TextureScale.calculateFitDimension(sprite.width(), sprite.height(), 160.0f, 160.f);
                ImGui.image(textureID, previewImageSize.x, previewImageSize.y,
                        textureCoordinates[2].x, textureCoordinates[0].y,
                        textureCoordinates[0].x, textureCoordinates[2].y);
                ImGui.text("Width: " + sprite.width());
                ImGui.text("Height: " + sprite.height());
                ImGui.endTooltip();
            }
            if (ImGui.beginDragDropSource()) {
                SpriteDragDropPayload.setPayload(sprite);
                ImGui.setDragDropPayload(SpriteDragDropPayload.getPayloadType(), sprite);
                ImGui.text("Sheet: " + sprite.textureReference());
                ImGui.text("Index: " + i);
                Vector2f previewImageSize = TextureScale.calculateFitDimension(sprite.width(), sprite.height(), 80.0f, 80.f);
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

    private static void searchFilter(String filterTerm) {
        if (filterTerm == null || !filterChanged) return;
        String sanctioned = filterTerm.trim().toLowerCase();
        filteredSpriteSheetList.clear();
        if (sanctioned.isEmpty()) {
            filteredSpriteSheetList.putAll(categorizedSpriteSheetList);
            filterChanged = false;
            return;
        }
        // Basically this filter the sheet map with matching filter term, then filter out the tab map where there is no sheet map inside.
        filteredSpriteSheetList.putAll(categorizedSpriteSheetList.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, tab -> tab.getValue().entrySet().stream()
                        .filter(header -> header.getKey().toLowerCase().contains(sanctioned))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                ))
                .entrySet().stream().filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        filterChanged = false;
    }

    private static void loadCategorizedSheet() {
        if (!Project.loaded()) return;
        removedSkip = false;
        ProjectData project = Project.currentProject();
        categorizedSpriteSheetList.clear();
        for (Map.Entry<String, Map<String, ProjectSheet>> categories : project.sheets().entrySet()) {
            String category = categories.getKey();
            Map<String, SpriteSheet> categorySheets = new HashMap<>();
            for (Map.Entry<String, ProjectSheet> sheets : categories.getValue().entrySet()) {
                String name = sheets.getKey();
                ProjectSheet sheet = sheets.getValue();
                String path = sheet.metadata();
                SpriteSheet spriteSheet = AssetManager.getSpriteSheet(path);
                if (spriteSheet != null) categorySheets.put(name, spriteSheet);
            }
            if (!categorySheets.isEmpty()) categorizedSpriteSheetList.put(category, categorySheets);
        }

    }

    private static void loadSingleSprites() {
        if (!Project.loaded()) return;
        removedSkip = false;
        singleSprites.clear();
        Project.assetEntries().forEach(asset -> {
            String path = asset.metadata();
            if (path == null || path.isBlank()) return;
            SingleSprite unit = AssetManager.getSingleSprite(path);
            if (unit != null) singleSprites.put(asset, unit);
        });
    }
}

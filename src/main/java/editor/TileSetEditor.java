package editor;

import components.TileMap;
import editor.payload.SpriteDragDropPayload;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.texture.Sprite;
import render.texture.TileSet;
import utility.IdPool;

public class TileSetEditor {
    private static final IdPool ID_POOL = new IdPool(16777214, true);
    private static final float tileSetEditPercentage = 0.3f;
    private static final float indentW = 4.0f;

    private static TileMap editingTileMap;
    private static final Vector2i tileSize = new Vector2i(16);
    private static final Vector2i startPosition = new Vector2i();

    static void edit(TileMap tileMap) {
        if (tileMap != editingTileMap) {
            clearDialogData();
            editingTileMap = tileMap;
            TileSet tileSet = tileMap.getTileSet();
            if (tileSet != null) {
                tileSize.set(tileSet.getGridSize());
                startPosition.set(tileSet.getStartPosition());
            }
        }
    }

    private static void clearDialogData() {
        editingTileMap = null;
        ID_POOL.reset();
        tileSize.set(16);
        startPosition.set(0);
    }

    static void imgui() {
        if (editingTileMap == null) {
            ImGui.textWrapped("Select a Tile Map component from Inspector panel to start editing its details");
            return;
        }

        if (editingTileMap.gameObject == null || editingTileMap.gameObject.isRemoved() || editingTileMap.getUUID() == null) {
            clearDialogData();
            return;
        }

        float remainWidth = Math.max(240.0f, ImGui.getContentRegionAvailX() * tileSetEditPercentage);
        if (!ImGui.beginTable("##TSE_Table_Id", 2, ImGuiTableFlags.BordersV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvail())) return;
        ImGui.tableSetupColumn("##TileSetEdit_Column", ImGuiTableColumnFlags.WidthFixed, remainWidth);
        ImGui.tableSetupColumn("##TileSetImage_Column", ImGuiTableColumnFlags.WidthStretch);

        ImGui.tableNextColumn();
        renderTileSetEdit();
        ImGui.tableNextColumn();
        renderTileSetImage();
        ImGui.endTable();
        ID_POOL.reset();
    }

    private static void renderTileSetEdit() {
        if (editingTileMap == null) return;

        TileSet tileSet = editingTileMap.getTileSet();
        if (tileSet == null) {
            createNewTileSet();
            return;
        }

        if (!ImGui.beginChild("##TileSet_Params_Edit_Region")) {
            ImGui.textWrapped("Failed to create region for tile set params");
            return;
        }

        tileSize.set(tileSet.getGridSize());
        startPosition.set(tileSet.getStartPosition());

        ImGui.text("Tile size:");
        ImGui.indent(indentW);
        ImBoolean sizeChanged = new ImBoolean(false);
        tileSize.x = inputInt("Width", tileSize.x, 1, sizeChanged);
        tileSize.y = inputInt("Height", tileSize.y, 1, sizeChanged);
        ImGui.unindent(indentW);

        ImGui.separator();
        ImGui.text("Start position offset:");
        ImGui.indent(indentW);
        ImBoolean startOffsetChanged = new ImBoolean(false);
        startPosition.x = inputInt("X offset", startPosition.x, 0, startOffsetChanged);
        startPosition.y = inputInt("Y offset", startPosition.y, 0, startOffsetChanged);
        ImGui.unindent(indentW);

        ImGui.endChild();

        if (sizeChanged.get()) tileSet.setGridSize(tileSize);
        if (startOffsetChanged.get()) tileSet.setStartPosition(startPosition);
    }

    private static void renderTileSetImage() {
        if (editingTileMap == null || editingTileMap.getTileSet() == null) return;

        TileSet tileSet = editingTileMap.getTileSet();
        if (tileSet.getTextureID() <= -1) return;
        Sprite sprite = tileSet.getTileSetSprite();
        if (sprite == null) return;

        if (!ImGui.beginChild("##TileSet_Image_Edit_Region", ImGui.getContentRegionAvail(), ImGuiChildFlags.None, ImGuiWindowFlags.HorizontalScrollbar)) {
            ImGui.textWrapped("Failed to create region for tile image");
            return;
        }

        int textureID = sprite.getTextureID();
        float w = sprite.getWidth();
        float h = sprite.getHeight();
        Vector2f[] textureCoordinates = sprite.getTextureCoordinates();

        ImGui.image(textureID, w, h,
                textureCoordinates[2].x, textureCoordinates[0].y,
                textureCoordinates[0].x, textureCoordinates[2].y
        );

        ImGui.endChild();
    }

    private static void createNewTileSet() {
        if (!ImGui.beginChild("##Drag_Drop_Tile_Set_Region", ImGui.getContentRegionAvail())) return;
        ImGui.beginDisabled();
        ImGui.textWrapped("Drag and drop an image or sprite here to create new tile set");
        ImGui.endDisabled();
        ImGui.endChild();

        if (ImGui.beginDragDropTarget()) {
            if (ImGui.isWindowHovered()) {
                ImDrawList drawList = ImGui.getWindowDrawList();
                ImVec2 min = ImGui.getItemRectMin();
                ImVec2 max = ImGui.getItemRectMax();
                drawList.addRect(min, max, ImGui.colorConvertFloat4ToU32(0.2f, 0.7f, 0.2f, 0.8f), 0 , 0 , 2);
            }

            Object payLoad = ImGui.acceptDragDropPayload(SpriteDragDropPayload.getPayloadType());

            if (payLoad == null) {
                ImGui.endDragDropTarget();
                return;
            }

            Sprite dropSprite = SpriteDragDropPayload.getPayload();
            if (dropSprite != null) {
                Vector2i gridSize = new Vector2i(16);
                Vector2i startPosition = new Vector2i();

                TileSet tileSet = new TileSet(gridSize, startPosition, dropSprite);
                if (editingTileMap != null) editingTileMap.setTileSet(tileSet);
            }

            ImGui.endDragDropTarget();
        }
    }

    private static int inputInt(String label, int target, int minValue, ImBoolean trackingFlag) {
        String id = label + "_" + "TSE" + ID_POOL.newId();
        ImGui.pushID(id);
        final boolean modified;
        final ImInt destination = new ImInt(target);

        modified = ImGui.inputInt(label, destination, 0, 0);

        if (modified) {
            target = Math.max(destination.get(), minValue);
            trackingFlag.set(true);
        }

        ImGui.popID();
        return target;
    }
}

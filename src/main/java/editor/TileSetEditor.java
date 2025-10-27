package editor;

import components.TileMap;
import imgui.ImGui;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;

public class TileSetEditor {
    private static TileMap editingTileMap;
    private static final float tileSetEditPercentage = 0.3f;

    static void edit(TileMap tileMap) {
        if (tileMap != editingTileMap) {
            clearDialogData();
            editingTileMap = tileMap;
        }
    }

    private static void clearDialogData() {
        editingTileMap = null;
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
        ImGui.textWrapped("Controls");
        ImGui.tableNextColumn();
        ImGui.textWrapped("Image");
        ImGui.endTable();
    }

    private static void renderTileSetEdit() {

    }

    private static void renderTileSetImage() {

    }
}

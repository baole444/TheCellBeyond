package editor;

import components.TileMap;
import imgui.ImGui;
import imgui.flag.ImGuiTableFlags;

public class TileSetEditor {
    private static TileMap editingTileMap;

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

        /*

        if (!ImGui.beginTable("##TSE_Table_Id", 2, ImGuiTableFlags.BordersV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvail())) return;


        ImGui.endTable();

         */
    }
}

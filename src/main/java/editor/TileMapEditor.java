package editor;

import components.TileMap;

class TileMapEditor {
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
}

package editor;

import components.TileMap;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImFloat;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.texture.Sprite;
import render.texture.Tile;
import render.texture.TileSet;

import java.util.*;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public class TileMapEditor {
    private enum Mode {
        Draw,
        Select,
        Erase
    }

    private static final float modeRegionReserve = ImGui.getFrameHeightWithSpacing();
    private static final float modeSelectableSize = 20.0f;

    private static Mode editingMode = Mode.Select;

    private static final int bgSquareSize = 32;
    private static final int lightSquareColor = ImGui.getColorU32(0.4f, 0.4f, 0.4f, 0.5f);
    private static final int darkSquareColor = ImGui.getColorU32(0.2f, 0.2f, 0.2f, 0.5f);

    private static final int tileHighlightColor = ImGui.getColorU32(1.0f, 1.0f, 0.5f, 0.75f);
    private static final int selectedTileColor = ImGui.getColorU32(0.5f, 1.0f, 1.0f, 1.0f);
    private static final float selectedThickness = 2.0f;

    private static TileMap editingTileMap;
    private static float zoom = 1.0f;

    private static final List<Tile> selectedTiles = new ArrayList<>();

    static void edit(TileMap tileMap) {
        if (tileMap != editingTileMap) {
            clearDialogData();
            editingTileMap = tileMap;
        }
    }

    private static void clearDialogData() {
        selectedTiles.clear();
        editingTileMap = null;
        zoom = 1.0f;
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

        if (!ImGui.beginChild("##TME_Controller_Region", 0.0f, modeRegionReserve + modeSelectableSize, true)) return;
        if (ImGui.beginTable("##TME_Mode_Table", 3, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingFixedFit)) {
            ImGui.tableSetupColumn("##TME_Select_Draw_Selectable_Column", ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableSetupColumn("##TME_Erase_selectable_Column", ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableSetupColumn("##TME_Zoom_Column", ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableNextColumn();
            boolean isSelectionMode = editingMode == Mode.Select;
            if (ImEditorGui.selectableIcon("Selection Mode##TME_Select_Mode_Selectable", EditorIcons.Icons.Select, "Click to toggle tile selection mode", isSelectionMode, modeSelectableSize, modeSelectableSize)) {
                editingMode = Mode.Select;
            }

            ImGui.sameLine();

            boolean isDrawMode = editingMode == Mode.Draw;
            if (ImEditorGui.selectableIcon("Draw Mode##TME_Draw_Mode_Selectable", EditorIcons.Icons.Edit, "Click to toggle tile draw mode", isDrawMode, modeSelectableSize, modeSelectableSize)) {
                editingMode = isDrawMode ? Mode.Select : Mode.Draw;
            }

            ImGui.tableNextColumn();
            boolean isEraserMode = editingMode == Mode.Erase;
            if (ImEditorGui.selectableIcon("Eraser Mode##TME_Eraser_Mode_Selectable", EditorIcons.Icons.Eraser, "Click to toggle tile eraser mode", isEraserMode, modeSelectableSize, modeSelectableSize)) {
                editingMode = isEraserMode ? Mode.Select : Mode.Erase;
            }

            ImGui.tableNextColumn();
            ImFloat z = new ImFloat(zoom);
            ImGui.pushItemWidth(ImGui.calcTextSizeX("+AAA.AAA"));
            if (ImGui.inputFloat("##Zoom_level_Direct", z, 0.0f, 0.0f)) zoom = Math.max(0.1f, z.get());
            ImGui.popItemWidth();
            ImGui.sameLine();
            float[] val = {zoom};
            if (ImGui.sliderFloat("Zoom##TSE_Zoom_Control", val, 0.1f, 4.0f)) {
                zoom = val[0];
            }

            ImGui.endTable();
        }
        ImGui.endChild();

        if (!ImGui.beginChild("##TME_Image_Region", ImGuiChildFlags.Border, ImGuiWindowFlags.HorizontalScrollbar)) return;
        renderTileSetImage();
        ImGui.endChild();
    }

    private static void renderTileSetImage() {
        if (editingTileMap == null || editingTileMap.getTileSet() == null) return;

        TileSet tileSet = editingTileMap.getTileSet();
        if (tileSet.getTextureID() <= -1) return;
        Sprite sprite = tileSet.getTileSetSprite();
        if (sprite == null) return;

        if (!ImGui.beginChild("##TileSet_Image_Edit_Region", ImGui.getContentRegionAvail(), ImGuiChildFlags.None, ImGuiWindowFlags.HorizontalScrollbar)) return;

        int textureID = sprite.getTextureID();
        float w = sprite.getWidth() * zoom;
        float h = sprite.getHeight() * zoom;
        Vector2f[] textureCoordinates = sprite.getTextureCoordinates();

        drawTransparentBackground(w, h);
        ImVec2 cursorScreenPos = ImGui.getCursorScreenPos();
        ImGui.image(textureID, w, h,
                textureCoordinates[2].x, textureCoordinates[0].y,
                textureCoordinates[0].x, textureCoordinates[2].y
        );
        drawTileHighLight(tileSet, cursorScreenPos);
        drawSelectedTiles(tileSet, cursorScreenPos);
        if (ImGui.isItemClicked(GLFW_MOUSE_BUTTON_1)) toggleTile(tileSet, cursorScreenPos, w, h);
        ImGui.endChild();
    }

    private static void drawTransparentBackground(float width, float height) {
        ImVec2 cursorPos = ImGui.getCursorScreenPos();
        ImDrawList drawList = ImGui.getWindowDrawList();
        float squareSize = bgSquareSize * zoom;
        int squareXCount = (int) Math.ceil(width / squareSize);
        int squareYCount = (int) Math.ceil(height / squareSize);

        ImVec2 rectMin = new ImVec2();
        ImVec2 rectMax = new ImVec2();

        for (int row = 0; row < squareYCount; row++) {
            for (int column = 0; column < squareXCount; column++) {
                boolean isLight = (row + column) % 2 == 0;
                int color = isLight ? lightSquareColor : darkSquareColor;

                float rectX = cursorPos.x + (column * squareSize);
                float rectY = cursorPos.y + (row * squareSize);
                float rectW = Math.min(squareSize, width - (column * squareSize));
                float rectH = Math.min(squareSize, height - (row * squareSize));
                rectMin.set(rectX, rectY);
                rectMax.set(rectX + rectW, rectY + rectH);

                drawList.addRectFilled(rectMin, rectMax, color);
            }
        }
    }

    private static void drawSelectedTiles(TileSet tileSet, ImVec2 cursorScreenPos) {
        if (selectedTiles.isEmpty()) return;
        HashSet<Vector2i> tileCoordinates = new HashSet<>();
        for (Tile tile : selectedTiles) {
            if (tile != null && tile.setCoordinate != null) tileCoordinates.add(tile.setCoordinate);
        }

        if (tileCoordinates.isEmpty()) return;
        Vector2i gridSize = tileSet.getGridSize();
        Vector2i startPos = tileSet.getStartPosition();

        ImDrawList drawList = ImGui.getWindowDrawList();

        float gridW = gridSize.x * zoom;
        float gridH = gridSize.y * zoom;
        float startX = startPos.x * zoom;
        float startY = startPos.y * zoom;

        ImVec2 min = new ImVec2();
        ImVec2 max = new ImVec2();

        for (Vector2i tile: tileCoordinates) {
            float x = cursorScreenPos.x + startX + tile.x * gridW;
            float y = cursorScreenPos.y + startY + tile.y * gridH;
            float right = x + gridW;
            float bottom = y + gridH;

            min.set(x, y);
            max.set(right, bottom);
            drawList.addRect(min, max, selectedTileColor, 0.0f, selectedThickness);
        }
    }

    private static void drawTileHighLight(TileSet tileSet, ImVec2 cursorScreenPos) {
        if (tileSet == null) return;

        List<Tile> tiles = tileSet.getTiles();
        if (tiles.isEmpty()) return;

        Vector2i gridSize = tileSet.getGridSize();
        Vector2i startPos = tileSet.getStartPosition();
        HashSet<Vector2i> tileCoordinates = tileSet.getTileCoordinates();

        if (tileCoordinates.isEmpty()) return;

        ImDrawList drawList = ImGui.getWindowDrawList();

        float gridW = gridSize.x * zoom;
        float gridH = gridSize.y * zoom;
        float startX = startPos.x * zoom;
        float startY = startPos.y * zoom;

        ImVec2 start = new ImVec2();
        ImVec2 end = new ImVec2();

        for (Vector2i tile : tileCoordinates) {
            float x = cursorScreenPos.x + startX + tile.x * gridW;
            float y = cursorScreenPos.y + startY + tile.y * gridH;
            float right = x + gridW;
            float bottom = y + gridH;

            boolean hasTop = hasNeighbouringTile(tileCoordinates, new Vector2i(tile.x, tile.y - 1));
            boolean hasRight = hasNeighbouringTile(tileCoordinates, new Vector2i(tile.x + 1, tile.y));
            boolean hasBottom = hasNeighbouringTile(tileCoordinates, new Vector2i(tile.x, tile.y + 1));
            boolean hasLeft = hasNeighbouringTile(tileCoordinates, new Vector2i(tile.x - 1, tile.y));

            if (!hasTop) {
                start.set(x, y);
                end.set(right, y);
                drawList.addLine(start, end, tileHighlightColor);
            }

            if (!hasRight) {
                start.set(right, y);
                end.set(right, bottom);
                drawList.addLine(start, end, tileHighlightColor);
            }

            if (!hasBottom) {
                start.set(x, bottom);
                end.set(right, bottom);
                drawList.addLine(start, end, tileHighlightColor);
            }

            if (!hasLeft) {
                start.set(x, y);
                end.set(x, bottom);
                drawList.addLine(start, end, tileHighlightColor);
            }
        }
    }

    private static boolean hasNeighbouringTile(Set<Vector2i> tiles, Vector2i coordinate) {
        for (Vector2i tile : tiles) {
            if (tile.x == coordinate.x && tile.y == coordinate.y) return true;
        }

        return false;
    }

    private static void toggleTile(TileSet tileSet, ImVec2 cursorScreenPos, float width, float height) {
        if (editingMode == null || editingMode == Mode.Erase) return;

        ImVec2 mousePos = ImGui.getMousePos();
        float relativeX = mousePos.x - cursorScreenPos.x;
        float relativeY = mousePos.y - cursorScreenPos.y;

        Vector2i gridSize = tileSet.getGridSize();
        Vector2i startPos = tileSet.getStartPosition();

        float startX = startPos.x * zoom;
        float startY = startPos.y * zoom;
        float gridW = gridSize.x * zoom;
        float gridH = gridSize.y * zoom;

        if (relativeX < startX || relativeY < startY || relativeX > width || relativeY > height) return;

        float clickedPixelX = relativeX - startX;
        float clickedPixelY = relativeY - startY;

        int gridX = (int) (clickedPixelX / gridW);
        int gridY = (int) (clickedPixelY / gridH);

        if (gridX < 0 || gridY < 0) return;
        Vector2i gridCoordinate = new Vector2i(gridX, gridY);

        Tile tile = tileSet.getTile(gridCoordinate);
        if (tile == null) return;

        if (!selectedTiles.contains(tile)) {
            selectedTiles.add(tile);
            return;
        }

        selectedTiles.remove(tile);
    }
}

package editor;

import TheCellBeyond.TileMap;
import editor.components.EditorTileMapGrid;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImFloat;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.texture.Sprite;
import render.texture.Tile;
import render.texture.TileSet;

import java.util.ArrayList;
import java.util.List;

public final class TileMapEditor {
    private enum Mode {
        Draw,
        Select,
        Erase
    }

    private static final float modeRegionReserve = ImGui.getFrameHeightWithSpacing();
    private static final float modeSelectableSize = 20.0f;
    private static Mode editingMode = Mode.Select;
    private static TileMap editingTileMap;
    private static float zoom = 1.0f;
    private static final List<Tile> selectedTiles = new ArrayList<>();
    private static final TileEditorRenderCache renderCache = new TileEditorRenderCache();
    private static boolean isBoxSelection = false;
    private static Vector2i selectionStart = null;
    private static Vector2i selectionEnd = null;

    public static TileMap getEditingTileMap() {
        return editingTileMap;
    }

    public static List<Tile> getSelectedTiles() {
        return new ArrayList<>(selectedTiles);
    }

    public static void clearSelectedTiles() {
        selectedTiles.clear();
        TileEditorRender.markSelectionDirty(renderCache);
    }

    public static void escapeMode() {
        editingMode = null;
        EditorTileMapGrid.hide();
    }

    static void edit(TileMap tileMap) {
        if (tileMap == editingTileMap) return;
        clearDialogData();
        editingTileMap = tileMap;
    }

    public static boolean isSelectMode() {
        if (editingMode == null) return false;
        return editingMode.equals(Mode.Select);
    }

    public static boolean isDrawMode() {
        if (editingMode == null) return false;
        return editingMode.equals(Mode.Draw);
    }

    public static boolean isEraseMode() {
        if (editingMode == null) return false;
        return editingMode.equals(Mode.Erase);
    }

    static void clearDialogData() {
        EditorTileMapGrid.hide();
        editingMode = null;
        selectedTiles.clear();
        TileEditorRender.markSelectionDirty(renderCache);
        renderCache.reset();
        editingTileMap = null;
        zoom = 1.0f;
    }

    static void imgui() {
        if (editingTileMap == null) {
            ImGui.textWrapped("Select a Tile Map component from Inspector panel to start editing its details");
            return;
        }
        if (editingTileMap.isDestroyed() || editingTileMap.getUUID() == null) {
            clearDialogData();
            return;
        }
        TileSet tileSet = editingTileMap.tileSet();
        TileEditorRender.checkInvalidation(renderCache, tileSet, selectedTiles);
        TileEditorRender.tickBuild(renderCache, tileSet, selectedTiles);
        boolean loading = TileEditorRender.notReady(renderCache);
        if (!ImGui.beginChild("##TME_Controller_Region", 0.0f, modeRegionReserve + modeSelectableSize, true)) {
            ImGui.endChild();
            return;
        }
        if (loading) ImGui.beginDisabled();
        if (ImGui.beginTable("##TME_Mode_Table", 4, ImGuiTableFlags.SizingFixedFit)) {
            ImGui.tableSetupColumn("##TME_Select_Mode_Selectable_Column", ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableSetupColumn("##TME_Draw_Mode_Selectable_Column");
            ImGui.tableSetupColumn("##TME_Erase_Mode_selectable_Column", ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableSetupColumn("##TME_Zoom_Mode_Column", ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableNextColumn();
            boolean isSelectionMode = editingMode == Mode.Select;
            if (EditorWidget.selectableIcon("Selection Mode##TME_Select_Mode_Selectable", EditorIcons.Icons.Select, "Click to toggle tile selection mode", isSelectionMode, modeSelectableSize, modeSelectableSize)) {
                editingMode = isSelectionMode ? null : Mode.Select;
                EditorTileMapGrid.hide();
            }
            ImGui.tableNextColumn();
            boolean isDrawMode = editingMode == Mode.Draw;
            if (EditorWidget.selectableIcon("Draw Mode##TME_Draw_Mode_Selectable", EditorIcons.Icons.EditPen, "Click to toggle tile draw mode", isDrawMode, modeSelectableSize, modeSelectableSize)) {
                editingMode = isDrawMode ? null : Mode.Draw;
                EditorTileMapGrid.draw(!isDrawMode);
            }
            ImGui.tableNextColumn();
            boolean isEraserMode = editingMode == Mode.Erase;
            if (EditorWidget.selectableIcon("Eraser Mode##TME_Eraser_Mode_Selectable", EditorIcons.Icons.Eraser, "Click to toggle tile eraser mode", isEraserMode, modeSelectableSize, modeSelectableSize)) {
                editingMode = isEraserMode ? null : Mode.Erase;
                EditorTileMapGrid.draw(!isEraserMode);
            }
            ImGui.tableNextColumn();
            if (ImGui.beginTable("##TME_TileSet_Sprite_Zoom_Control_Layout", 4, ImGuiTableFlags.SizingFixedFit)) {
                ImGui.tableSetupColumn("##TME_TileSet_Sprite_Zoom_Control_Label_Column", ImGuiTableColumnFlags.WidthFixed);
                ImGui.tableSetupColumn("##TME_TileSet_Sprite_Zoom_Control_Input_Column", ImGuiTableColumnFlags.WidthFixed);
                ImGui.tableSetupColumn("##TME_TilSet_Sprite_Zoom_Control_Slider_Column", ImGuiTableColumnFlags.WidthStretch);
                ImGui.tableSetupColumn("##TME_TileSet_Sprite_Zoom_Control_Reset_Column", ImGuiTableColumnFlags.WidthFixed);
                ImGui.tableNextColumn();
                ImGui.setCursorPosY(ImGui.getCursorPosY() + (ImGui.getFrameHeightWithSpacing() - ImGui.getTextLineHeightWithSpacing()) / 2.0f);
                ImGui.text("Zoom:");
                ImGui.tableNextColumn();
                ImFloat z = new ImFloat(zoom);
                ImGui.setNextItemWidth(ImGui.calcTextSizeX("+AAA.AAA"));
                if (ImGui.inputFloat("##TME_Zoom_level_Direct_Input", z, 0.0f, 0.0f)) zoom = Math.clamp(z.get(), 0.1f, 4.0f);
                if (ImGui.isItemHovered()) {
                    ImGui.beginTooltip();
                    ImGui.text("Enter the zoom level (1.0 -> 4.0)");
                    ImGui.endTooltip();
                }
                ImGui.tableNextColumn();
                float[] val = {zoom};
                ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
                if (ImGui.sliderFloat("##TME_Zoom_level_Slider_Control", val, 0.1f, 4.0f)) {
                    zoom = val[0];
                }
                if (ImGui.isItemHovered()) {
                    ImGui.beginTooltip();
                    ImGui.text("Slide the bar to change zoom level");
                    ImGui.endTooltip();
                }
                ImGui.tableNextColumn();
                if (EditorWidget.iconButton("##TME_Zoom_level_Control_Reset_Button", EditorIcons.Icons.Reset, "Reset zoom to 1.0")) zoom = 1.0f;
                ImGui.endTable();
            }
            ImGui.endTable();
        }
        if (loading) ImGui.endDisabled();
        ImGui.endChild();
        if (ImGui.beginChild("##TME_Image_Region", ImGuiChildFlags.Borders, ImGuiWindowFlags.HorizontalScrollbar)) {
            if (loading) TileEditorRender.drawLoadingPlaceholder();
            else renderTileSetImage();
        }
        ImGui.endChild();
    }

    private static void renderTileSetImage() {
        if (editingTileMap == null || editingTileMap.tileSet() == null) return;
        TileSet tileSet = editingTileMap.tileSet();
        if (tileSet.textureID() <= -1) return;
        Sprite sprite = tileSet.tileSetSprite();
        if (sprite == null) return;
        if (!ImGui.beginChild("##TileSet_Image_Edit_Region", ImGui.getContentRegionAvail(), ImGuiChildFlags.None, ImGuiWindowFlags.HorizontalScrollbar)) {
            ImGui.endChild();
            return;
        }
        int textureID = sprite.getTextureID();
        float w = sprite.getWidth() * zoom;
        float h = sprite.getHeight() * zoom;
        Vector2f[] textureCoordinates = sprite.getTextureCoordinates();
        ImVec2 cursorScreenPos = ImGui.getCursorScreenPos();
        TileEditorRender.drawCheckerboard(renderCache, cursorScreenPos, zoom);
        ImGui.image(textureID, w, h,
                textureCoordinates[2].x, textureCoordinates[0].y,
                textureCoordinates[0].x, textureCoordinates[2].y
        );
        TileEditorRender.drawTileOutlines(renderCache, cursorScreenPos, zoom);
        TileEditorRender.drawSelectedTiles(renderCache, cursorScreenPos, zoom);
        handleTileSelection(tileSet, cursorScreenPos, w, h);
        ImGui.endChild();
    }

    private static void handleTileSelection(TileSet tileSet, ImVec2 cursorScreenPos, float width, float height) {
        if (editingMode == null || editingMode == Mode.Erase) return;
        boolean isItemHovered = ImGui.isItemHovered();
        boolean isMouseDown = ImGui.isMouseDown(ImGuiMouseButton.Left);
        boolean isMouseClick = ImGui.isMouseClicked(ImGuiMouseButton.Left);
        boolean isShift = ImGui.getIO().getKeyShift();
        boolean isControl = ImGui.getIO().getKeyCtrl();
        if (handleBoxSelectionShortcut(tileSet, cursorScreenPos, width, height, isItemHovered, isMouseClick, isShift, isControl)) return;
        if (isItemHovered && isMouseClick && !isShift) {
            selectedTiles.clear();
            TileEditorRender.markSelectionDirty(renderCache);
            isBoxSelection = true;
            selectionStart = getGridCoordinate(tileSet, cursorScreenPos, width, height);
            selectionEnd = selectionStart;
            if (selectionStart != null) selectTiles(tileSet, selectionStart, selectionEnd);
        }
        if (isBoxSelection && isMouseDown) {
            Vector2i current = getGridCoordinate(tileSet, cursorScreenPos, width, height);
            if (current != null && !current.equals(selectionEnd)) {
                selectionEnd = current;
                selectedTiles.clear();
                TileEditorRender.markSelectionDirty(renderCache);
                selectTiles(tileSet, selectionStart, selectionEnd);
            }
        }
        if (isMouseDown || !isBoxSelection) return;
        isBoxSelection = false;
        selectionStart = null;
        selectionEnd = null;
    }

    private static boolean handleBoxSelectionShortcut(TileSet tileSet, ImVec2 cursorScreenPos, float width, float height, boolean isItemHovered, boolean isMouseClick, boolean isShift, boolean isControl) {
        if (!isItemHovered || !isMouseClick) return false;
        if (!isShift && !isControl) return false;
        if (isShift && !isControl) {
            Vector2i clicked = getGridCoordinate(tileSet, cursorScreenPos, width, height);
            if (clicked == null) return true;
            if (!selectedTiles.isEmpty()) {
                Tile first = selectedTiles.getFirst();
                Vector2i coordinate = first.setCoordinate;
                selectedTiles.clear();
                selectTiles(tileSet, coordinate, clicked);
            } else {
                Tile tile = tileSet.tile(clicked);
                if (tile != null) selectedTiles.add(tile);
            }
            TileEditorRender.markSelectionDirty(renderCache);
            return true;
        }
        if (isShift) return false;
        Vector2i clicked = getGridCoordinate(tileSet, cursorScreenPos, width, height);
        if (clicked == null) return true;
        Tile tile = tileSet.tile(clicked);
        if (tile == null) return true;
        if (!selectedTiles.remove(tile)) selectedTiles.add(tile);
        TileEditorRender.markSelectionDirty(renderCache);
        return true;
    }

    private static Vector2i getGridCoordinate(TileSet tileSet, ImVec2 cursorScreenPos, float width, float height) {
        ImVec2 mousePos = ImGui.getMousePos();
        float relativeX = mousePos.x - cursorScreenPos.x;
        float relativeY = mousePos.y - cursorScreenPos.y;
        Vector2i gridSize = tileSet.gridSize();
        Vector2i startPos = tileSet.startPosition();
        float startX = startPos.x * zoom;
        float startY = startPos.y * zoom;
        float gridW = gridSize.x * zoom;
        float gridH = gridSize.y * zoom;
        if (relativeX < startX || relativeY < startY || relativeX > width || relativeY > height) return null;
        float clickedPixelX = relativeX - startX;
        float clickedPixelY = relativeY - startY;
        int gridX = (int) (clickedPixelX / gridW);
        int gridY = (int) (clickedPixelY / gridH);
        if (gridX < 0 || gridY < 0) return null;
        return new Vector2i(gridX, gridY);
    }

    private static void selectTiles(TileSet tileSet, Vector2i start, Vector2i end) {
        int xStep = start.x <= end.x ? 1 : -1;
        int yStep = start.y <= end.y ? 1 : -1;
        boolean xL2R = xStep > 0;
        boolean yL2R = yStep > 0;
        Vector2i coordinate = new Vector2i();
        for (int y = start.y; yL2R ? y <= end.y : y >= end.y; y += yStep) {
            for (int x = start.x; xL2R ? x <= end.x : x >= end.x; x += xStep) {
                Tile tile = tileSet.tile(coordinate.set(x, y));
                if (tile != null && !selectedTiles.contains(tile)) selectedTiles.add(tile);
            }
        }
    }
}

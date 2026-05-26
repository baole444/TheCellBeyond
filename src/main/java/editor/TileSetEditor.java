package editor;

import TheCellBeyond.TileMap;
import editor.payload.SpriteDragDropPayload;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImFloat;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.texture.Sprite;
import render.texture.Tile;
import render.texture.TileSet;
import utility.TextureScale;
import utility.log.EngineLog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TileSetEditor {
    private enum Mode {
        Add,
        Select,
        Erase
    }

    private static final EngineLog LOGGER = new EngineLog(TileSetEditor.class);
    private static final float modeRegionReserve = ImGui.getFrameHeightWithSpacing();
    private static final float modeSelectableSize = 20.0f;

    private static final Mode defaultMode = Mode.Add;
    private static Mode editingMode;

    private static final int bgSquareSize = 32;
    private static final int lightSquareColor = ImGui.getColorU32(0.4f, 0.4f, 0.4f, 0.5f);
    private static final int darkSquareColor = ImGui.getColorU32(0.2f, 0.2f, 0.2f, 0.5f);

    private static final int tileHighlightColor = ImGui.getColorU32(1.0f, 1.0f, 0.5f, 0.75f);
    private static final int selectedTileFillColor = ImGui.getColorU32(0.5f, 1.0f, 1.0f, 0.25f);
    private static final int selectedTileColor = ImGui.getColorU32(0.5f, 1.0f, 1.0f, 1.0f);
    private static final int firstSelectedTileColor = ImGui.getColorU32(1.0f, 0.5f, 0.5f, 1.0f);
    private static final int firstSelectedTileFillColor = ImGui.getColorU32(1.0f, 0.5f, 0.5f, 0.25f);
    private static final float selectedThickness = 2.0f;

    private static final float tileSetEditPercentage = 0.24f;
    private static float zoom = 1.0f;

    private static TileMap editingTileMap;

    private static final List<Tile> selectedTiles = new ArrayList<>();
    private static boolean isBoxSelection = false;
    private static Vector2i selectionStart = null;
    private static Vector2i selectionEnd = null;

    static void edit(TileMap tileMap) {
        if (tileMap != editingTileMap) {
            clearDialogData();
            editingTileMap = tileMap;
        }
    }

    static void clearDialogData() {
        selectedTiles.clear();
        editingTileMap = null;
        zoom = 1.0f;
        editingMode = defaultMode;
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
        float remainWidth = Math.max(120.0f, ImGui.getContentRegionAvailX() * tileSetEditPercentage);
        if (!ImGui.beginTable("##TSE_Main_Region_Layout", 3, ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvail())) return;
        ImGui.tableSetupColumn("##TSE_TileSet_SpriteList_Column", ImGuiTableColumnFlags.WidthFixed, remainWidth);
        ImGui.tableSetupColumn("##TSE_TileSet_Tile_Properties_Column", ImGuiTableColumnFlags.WidthFixed, remainWidth);
        ImGui.tableSetupColumn("##TSE_TileSet_Image_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableNextColumn();
        renderTileSetSpriteList();
        ImGui.tableNextColumn();
        renderTileProperties();
        ImGui.tableNextColumn();
        renderTileSetControl();
        renderTileSetImage();
        ImGui.endTable();
    }

    private static void renderTileSetSpriteList() {
        if (editingTileMap == null) return;
        if (!ImGui.beginChild("##TSE_TileSet_Sprite_List_Region", ImGui.getContentRegionAvail(), ImGuiChildFlags.Borders)) {
            ImGui.endChild();
            return;
        }
        TileSet tileSet = editingTileMap.tileSet();
        if (tileSet == null) {
            if (ImGui.button("Create new Tile set", ImGui.getContentRegionAvailX(), 0.0f)) editingTileMap.tileSet(new TileSet());
            ImGui.endChild();
            return;
        }
        Sprite mainSprite = tileSet.tileSetSprite();
        if (mainSprite == null) {
            ImGui.beginDisabled();
            ImGui.textWrapped("Drag and drop sprite here to add it to the Tile set");
            ImGui.endDisabled();
            ImGui.endChild();
            setTileSetSprite();
            return;
        }
        if (!ImGui.beginTable("##TSE_TileSet_Sprite_List_Selectable_Layout", 2, ImGuiTableFlags.SizingStretchProp)) {
            ImGui.endChild();
            return;
        }
        ImGui.tableSetupColumn("TSE_TileSet_Sprite_Selectable_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("TSE_TileSet_Remove_Sprite_Button_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableNextColumn();
        ImGui.beginGroup();
        ImVec2 cursorPos = ImGui.getCursorPos();
        float maxHeight = ImGui.getTextLineHeight() * 3.0f;
        ImGui.selectable("##TSE_Main_TileSet_Sprite_Selectable", false, ImGui.getContentRegionAvailX(), maxHeight);
        int textureId = mainSprite.getTextureID();
        float sWidth = mainSprite.getWidth();
        float sHeight = mainSprite.getHeight();
        Vector2f scaledSize = TextureScale.calculateFitSquare(sWidth, sHeight, maxHeight);
        Vector2f[] textureCoordinates = mainSprite.getTextureCoordinates();
        ImGui.setCursorPos(cursorPos);
        ImGui.image(textureId, scaledSize.x, scaledSize.y,
                textureCoordinates[2].x, textureCoordinates[0].y,
                textureCoordinates[0].x, textureCoordinates[2].y
        );
        ImGui.setCursorPos(cursorPos.x + maxHeight + 2.0f, cursorPos.y + Math.max(0.0f, (maxHeight - ImGui.getTextLineHeight() * 2.0f)));
        String path = mainSprite.getTexture().getCanonicalPath();
        path = path == null ? "Unknow texture" : path.substring(path.lastIndexOf("/") + 1);
        ImGui.text(path);
        ImGui.endGroup();
        ImGui.tableNextColumn();
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (maxHeight - ImGui.getTextLineHeight() * 2.0f));
        if (EditorWidget.iconButton("Delete##TSE_Main_TileSet_Sprite_Delete_Button", EditorIcons.Icons.Delete, "Delete this Sprite from Tile set")) {
            selectedTiles.clear();
            tileSet.tileSetSprite(null);
        }
        ImGui.endTable();
        ImGui.endChild();
    }

    private static void setTileSetSprite() {
        if (!ImGui.beginDragDropTarget()) return;
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
        if (dropSprite == null || editingTileMap == null) {
            ImGui.endDragDropTarget();
            return;
        }

        TileSet tileSet = editingTileMap.tileSet();
        if (tileSet == null) LOGGER.error("Internal state error: TileSet is null");
        else tileSet.tileSetSprite(dropSprite);

        ImGui.endDragDropTarget();
    }

    private static void renderTileProperties() {
        if (editingTileMap == null || editingTileMap.tileSet() == null || editingTileMap.tileSet().tileSetSprite() == null) return;
        if (!ImGui.beginChild("##TSE_Tile_Properties_Region", ImGui.getContentRegionAvail(), ImGuiChildFlags.Borders)) {
            ImGui.endChild();
            return;
        }

        EditorWidget.textCenterAlign("Tile Properties");
        ImGui.separator();
        if (!ImGui.beginChild("##TSE_Tile_Properties_Inner_Region", ImGui.getContentRegionAvail())) {
            ImGui.endChild();
            ImGui.endChild();
            return;
        }
        ImGui.spacing();

        if (selectedTiles.isEmpty()) {
            ImGui.textWrapped("Select one or more tiles to edit their properties");
            ImGui.endChild();
            ImGui.endChild();
            return;
        }

        Tile firstTile = selectedTiles.getFirst();
        if (firstTile == null) {
            ImGui.endChild();
            ImGui.endChild();
            return;
        }

        boolean openPhysic = ImGui.collapsingHeader("Physics##TSE_TileSet_Tile_Physics_Header_" + editingTileMap.getUUID());

        if (!openPhysic) {
            ImGui.endChild();
            ImGui.endChild();
            return;
        }

        TileCollisionShapeEditor.renderLayerCollisionShape(editingTileMap.tileSet(), firstTile, selectedTiles);
        ImGui.endChild();
        ImGui.endChild();
    }

    private static void renderTileSetControl() {
        if (!ImGui.beginChild("##TileSet_Edit_Mode_Region", new ImVec2(0.0f, 0.0f), ImGuiChildFlags.AutoResizeY | ImGuiChildFlags.Borders)) {
            ImGui.endChild();
            return;
        }

        if (!ImGui.beginTable("##TSE_Control_Table", 4, ImGuiTableFlags.SizingFixedFit)) {
            ImGui.endChild();
            return;
        }

        ImGui.tableSetupColumn("TSE_Select_Mode_Selectable_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("TSE_Remove_Mode_Selectable_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("TSE_Search_Tile_Selectable_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("TSE_Zoom_Control_Column", ImGuiTableColumnFlags.WidthStretch);

        ImGui.tableNextColumn();
        boolean isSelectionMode = editingMode == Mode.Select;
        if (EditorWidget.selectableIcon("Selection Mode##TSE_Select_Mode_Selectable", EditorIcons.Icons.Select, "Click to toggle tile selection mode", isSelectionMode, modeSelectableSize, modeSelectableSize)) {
            editingMode = isSelectionMode ? defaultMode : Mode.Select;
            selectedTiles.clear();
        }

        ImGui.tableNextColumn();
        boolean isEraseMode = editingMode == Mode.Erase;
        if (EditorWidget.selectableIcon("Eraser Mode##TSE_Eraser_Mode_Selectable", EditorIcons.Icons.Eraser, "Click to toggle tile removal mode", isEraseMode, modeSelectableSize, modeSelectableSize)) {
            editingMode = isEraseMode ? defaultMode : Mode.Erase;
            selectedTiles.clear();
        }

        ImGui.tableNextColumn();
        if (EditorWidget.iconButton("Find tiles##TSE_Find_Tile_Button", EditorIcons.Icons.Search, "Click to find tile automatically", modeSelectableSize, modeSelectableSize)) {
            TileSet set = editingTileMap.tileSet();
            if (set != null) set.findTiles();
            selectedTiles.clear();
        }

        ImGui.tableNextColumn();
        if (ImGui.beginTable("##TSE_TileSet_Sprite_Zoom_Control_Layout", 4, ImGuiTableFlags.SizingFixedFit)) {
            ImGui.tableSetupColumn("##TSE_TileSet_Sprite_Zoom_Control_Label_Column", ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableSetupColumn("##TSE_TileSet_Sprite_Zoom_Control_Input_Column", ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableSetupColumn("##TSE_TilSet_Sprite_Zoom_Control_Slider_Column", ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableSetupColumn("##TSE_TileSet_Sprite_Zoom_Control_Reset_Column", ImGuiTableColumnFlags.WidthFixed);

            ImGui.tableNextColumn();
            ImGui.setCursorPosY(ImGui.getCursorPosY() + (ImGui.getFrameHeightWithSpacing() - ImGui.getTextLineHeightWithSpacing()) / 2.0f);
            ImGui.text("Zoom:");

            ImGui.tableNextColumn();
            ImFloat z = new ImFloat(zoom);
            ImGui.setNextItemWidth(ImGui.calcTextSizeX("+AAA.AAA"));
            if (ImGui.inputFloat("##TSE_Zoom_level_Direct_Input", z, 0.0f, 0.0f)) zoom = Math.max(0.1f, Math.min(4.0f, z.get()));
            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.text("Enter the zoom level (1.0 -> 4.0)");
                ImGui.endTooltip();
            }

            ImGui.tableNextColumn();
            float[] val = {zoom};
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
            if (ImGui.sliderFloat("##TSE_Zoom_level_Slider_Control", val, 0.1f, 4.0f)) {
                zoom = val[0];
            }
            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.text("Slide the bar to change zoom level");
                ImGui.endTooltip();
            }

            ImGui.tableNextColumn();
            if (EditorWidget.iconButton("##TSE_Zoom_level_Control_Reset_Button", EditorIcons.Icons.Reset, "Reset zoom to 1.0")) zoom = 1.0f;
            ImGui.endTable();
        }

        ImGui.endTable();
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

        drawTransparentBackground(w, h);
        ImVec2 cursorScreenPos = ImGui.getCursorScreenPos();
        ImGui.image(textureID, w, h,
                textureCoordinates[2].x, textureCoordinates[0].y,
                textureCoordinates[0].x, textureCoordinates[2].y
        );
        drawTileHighLight(tileSet, cursorScreenPos);
        drawSelectedTiles(tileSet, cursorScreenPos);
        if (ImGui.isItemClicked(ImGuiMouseButton.Left)) toggleTileInTileSet(tileSet, cursorScreenPos, w, h);
        handleTileSelection(tileSet, cursorScreenPos, w, h);
        ImGui.endChild();
    }

    private static void toggleTileInTileSet(TileSet tileSet, ImVec2 cursorScreenPos, float width, float height) {
        if (editingMode == Mode.Select) return;

        ImVec2 mousePos = ImGui.getMousePos();
        float relativeX = mousePos.x - cursorScreenPos.x;
        float relativeY = mousePos.y - cursorScreenPos.y;

        Vector2i gridSize = tileSet.gridSize();
        Vector2i startPos = tileSet.startPosition();

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


        Tile tile = tileSet.tile(gridCoordinate);
        if (editingMode == Mode.Erase) {
            if (tile == null) return;
            tileSet.removeTile(gridCoordinate);
            LOGGER.debug("Removed tile (" + gridX + "," + gridY + ") from + " + tileSet);
            return;
        }

        if (editingMode == Mode.Add) {
            if (tile != null) return;
            tileSet.addTile(gridCoordinate);
            LOGGER.debug("Added tile (" + gridX + "," + gridY + ") to + " + tileSet);
        }

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
        List<Tile> tiles = new ArrayList<>(selectedTiles);
        if (tiles.isEmpty()) return;
        HashSet<Vector2i> tileCoordinates = new HashSet<>();
        Tile firstTile = tiles.getFirst();
        if (firstTile == null || firstTile.setCoordinate == null) return;
        Vector2i firstCoordinate = new Vector2i(firstTile.setCoordinate);

        Vector2i gridSize = tileSet.gridSize();
        Vector2i startPos = tileSet.startPosition();

        ImDrawList drawList = ImGui.getWindowDrawList();

        float gridW = gridSize.x * zoom;
        float gridH = gridSize.y * zoom;
        float startX = startPos.x * zoom;
        float startY = startPos.y * zoom;

        ImVec2 min = new ImVec2();
        ImVec2 max = new ImVec2();

        float x, y, right, bottom;

        for (Tile tile : tiles) {
            if (tile == null || tile.setCoordinate == null || tile.setCoordinate.equals(firstCoordinate)) continue;
            tileCoordinates.add(tile.setCoordinate);
        }
        if (!tileCoordinates.isEmpty()) {
            for (Vector2i tile: tileCoordinates) {
                x = cursorScreenPos.x + startX + tile.x * gridW;
                y = cursorScreenPos.y + startY + tile.y * gridH;
                right = x + gridW;
                bottom = y + gridH;

                min.set(x, y);
                max.set(right, bottom);
                drawList.addRectFilled(min, max, selectedTileFillColor, 0.0f);
                drawList.addRect(min, max, selectedTileColor, 0.0f, selectedThickness);
            }
        }

        x = cursorScreenPos.x + startX + firstCoordinate.x * gridW;
        y = cursorScreenPos.y + startY + firstCoordinate.y * gridH;
        right = x + gridW;
        bottom = y + gridH;

        min.set(x, y);
        max.set(right, bottom);
        drawList.addRectFilled(min, max, firstSelectedTileFillColor, 0.0f);
        drawList.addRect(min, max, firstSelectedTileColor, 0.0f, selectedThickness);
    }

    private static void drawTileHighLight(TileSet tileSet, ImVec2 cursorScreenPos) {
        if (tileSet == null) return;

        List<Tile> tiles = tileSet.tiles();
        if (tiles.isEmpty()) return;

        Vector2i gridSize = tileSet.gridSize();
        Vector2i startPos = tileSet.startPosition();
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

    private static void handleTileSelection(TileSet tileSet, ImVec2 cursorScreenPos, float width, float height) {
        if (editingMode != Mode.Select) return;

        boolean isItemHovered = ImGui.isItemHovered();
        boolean isMouseDown = ImGui.isMouseDown(ImGuiMouseButton.Left);
        boolean isMouseClick = ImGui.isMouseClicked(ImGuiMouseButton.Left);
        boolean isShift = ImGui.getIO().getKeyShift();
        boolean isControl = ImGui.getIO().getKeyCtrl();

        if (handleBoxSelectionShortcut(tileSet, cursorScreenPos, width, height, isItemHovered, isMouseClick, isShift, isControl)) return;

        if (isItemHovered && isMouseClick && !isShift) {
            selectedTiles.clear();
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
                selectTiles(tileSet, selectionStart, selectionEnd);
            }
        }

        if (!isMouseDown && isBoxSelection) {
            isBoxSelection = false;
            selectionStart = null;
            selectionEnd = null;
        }
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
            return true;
        }

        if (isShift) return false;

        Vector2i clicked = getGridCoordinate(tileSet, cursorScreenPos, width, height);
        if (clicked == null) return true;

        Tile tile = tileSet.tile(clicked);
        if (tile == null) return true;

        if (!selectedTiles.remove(tile)) selectedTiles.add(tile);
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

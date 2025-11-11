package editor;

import components.TileMap;
import editor.payload.SpriteDragDropPayload;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.texture.Sprite;
import render.texture.Tile;
import render.texture.TileSet;
import utility.IdPool;
import utility.log.EngineLog;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public class TileSetEditor {
    private enum Mode {
        Add,
        Select,
        Erase
    }

    private static final EngineLog LOGGER = new EngineLog(TileSetEditor.class);
    private static final float modeRegionReserve = ImGui.getFrameHeightWithSpacing();
    private static final float modeSelectableSize = 20.0f;
    private static final ImVec2 padding = ImGui.getStyle().getFramePadding();
    private static final Mode defaultMode = Mode.Add;
    private static Mode editingMode;
    private static final int bgSquareSize = 32;
    private static final int lightSquareColor = ImGui.getColorU32(0.4f, 0.4f, 0.4f, 0.5f);
    private static final int darkSquareColor = ImGui.getColorU32(0.2f, 0.2f, 0.2f, 0.5f);
    private static final IdPool ID_POOL = new IdPool(0, false);
    private static final float tileSetEditPercentage = 0.3f;
    private static final float indentW = 4.0f;
    private static float zoom = 1.0f;

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
        zoom = 1.0f;
        ID_POOL.reset();
        tileSize.set(16);
        startPosition.set(0);
        editingMode = defaultMode;
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
        if (!ImGui.beginTable("##TSE_Main_Region_Table", 2, ImGuiTableFlags.BordersV | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvail())) return;
        ImGui.tableSetupColumn("##TileSetEdit_Column", ImGuiTableColumnFlags.WidthFixed, remainWidth);
        ImGui.tableSetupColumn("##TileSetImage_Column", ImGuiTableColumnFlags.WidthStretch);

        ImGui.tableNextColumn();
        renderTileSetEdit();
        ImGui.tableNextColumn();
        renderTileSetControl();
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

    private static void renderTileSetControl() {
        if (!ImGui.beginChild("##TileSet_Edit_Mode_Region", 0.0f, modeRegionReserve + modeSelectableSize, ImGuiChildFlags.Border)) return;

        if (!ImGui.beginTable("##TSE_Control_Table", 3,ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingFixedFit)) {
            ImGui.endChild();
            return;
        }

        ImGui.tableSetupColumn("TSE_Modes_Selectable_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("TSE_Search_Tile_Selectable_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("TSE_Zoom_Control_Column", ImGuiTableColumnFlags.WidthStretch);

        ImGui.tableNextColumn();
        boolean isSelectionMode = editingMode == Mode.Select;
        if (ImEditorGui.selectableIcon("Selection Mode##TSE_Select_Mode_Selectable", EditorIcons.Icons.Select, "Click to toggle tile selection mode", isSelectionMode, modeSelectableSize, modeSelectableSize)) {
            editingMode = isSelectionMode ? defaultMode : Mode.Select;
        }

        ImGui.sameLine();

        boolean isEraseMode = editingMode == Mode.Erase;
        if (ImEditorGui.selectableIcon("Eraser Mode##TSE_ERaser_Mode_Selectable", EditorIcons.Icons.Eraser, "Click to toggle tile removal mode", isEraseMode, modeSelectableSize, modeSelectableSize)) {
            editingMode = isEraseMode ? defaultMode : Mode.Erase;
        }

        ImGui.tableNextColumn();
        if (ImEditorGui.iconButton("Find tiles##TSE_Find_Tile_Button", EditorIcons.Icons.Search, "Click to find tile automatically", modeSelectableSize, modeSelectableSize)) {
            TileSet set = editingTileMap.getTileSet();
            if (set != null) set.findTiles();

        }

        ImGui.tableNextColumn();
        ImFloat z = new ImFloat(zoom);
        ImGui.pushItemWidth(ImGui.calcTextSizeX("AAA.AAA"));
        if (ImGui.inputFloat("##Zoom_level_Direct", z, 0.0f, 0.0f)) zoom = z.get();
        ImGui.popItemWidth();
        ImGui.sameLine();
        float[] val = {zoom};
        if (ImGui.sliderFloat("Zoom##TSE_Zoom_Control", val, 0.1f, 4.0f)) {
            zoom = val[0];
        }

        ImGui.endTable();
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

        if (ImGui.isItemClicked(GLFW_MOUSE_BUTTON_1)) toggleTile(tileSet, cursorScreenPos, w, h);

        ImGui.endChild();
    }

    private static void toggleTile(TileSet tileSet, ImVec2 cursorScreenPos, float width, float height) {
        if (editingMode == Mode.Select) return;

        ImVec2 mousePos = ImGui.getMousePos();
        float relativeX = mousePos.x - cursorScreenPos.x;
        float relativeY = mousePos.y - cursorScreenPos.y;

        Vector2i gridSize = tileSet.getGridSize();
        Vector2i startPos = tileSet.getStartPosition();
        if (relativeX < startPos.x || relativeY < startPos.y || relativeX > width || relativeY > height) return;

        int clickedPixelX = (int) relativeX - startPos.x;
        int clickedPixelY = (int) relativeY - startPos.y;

        int gridX = clickedPixelX / gridSize.x;
        int gridY = clickedPixelY / gridSize.y;

        if (gridX < 0 || gridY < 0) return;
        Vector2i gridCoordinate = new Vector2i(gridX, gridY);


        Tile tile = tileSet.getTile(gridCoordinate);
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

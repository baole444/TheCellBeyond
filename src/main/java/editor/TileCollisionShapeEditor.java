package editor;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.texture.Tile;
import render.texture.TileSet;
import utility.TextureScale;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

class TileCollisionShapeEditor {
    private enum Mode {
        AddPolygon,
        EditPolygon,
        RemovePolygon,
    }

    private static final float tilePreviewSize = 80.0f;
    private static final int MinNode = 3;
    private static final int MaxNode = 8;
    private static final float NodeRadius = 5.0f;
    private static final float modeSelectableSize = 20.0f;

    private static final int tileBackgroundBorderColor = ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1.0f);
    private static final int tileBackgroundFillColor = ImGui.getColorU32(0.1f, 0.1f, 0.1f, 1.0f);
    private static final int NodeColor = ImGui.getColorU32(0.7f, 0.7f, 0.7f, 1.0f);
    private static final int NodeHoverColor = ImGui.getColorU32(0.9f, 0.9f, 0.75f, 1.0f);
    private static final int PolygonColor = ImGui.getColorU32(0.8f, 0.1f, 0.1f, 0.7f);
    private static final int PolygonFillColor = ImGui.getColorU32(0.8f, 0.1f, 0.1f, 0.4f);

    private static Tile editingTile = null;
    private static Mode editingMode = null;
    private static int draggingNodeIndex = -1;

    private static void resetData() {
        editingMode = null;
        editingTile = null;
        draggingNodeIndex = -1;
    }

    static void renderLayerCollisionShape(TileSet tileset, Tile tile) {
        if (tileset == null || tile == null) {
            resetData();
            return;
        }

        if (editingTile != tile) resetData();
        editingTile = tile;

        if (!ImGui.beginTable("##TSE_TCSE_Collision_Shape_Editor_Mode_layout", 4, ImGuiTableFlags.SizingFixedFit)) return;
        ImGui.tableSetupColumn("##TSE_TCSE_Collision_Shape_Add_Node_Mode_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##TSE_TCSE_Collision_Shape_Remove_Node_Mode_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##TSE_TCSE_Collision_Shape_Add_Node_Mode_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##TSE_TCSE_Collision_Shape_Reset_Node_Column", ImGuiTableColumnFlags.WidthFixed);

        ImGui.tableNextColumn();
        boolean isAddMode = editingMode == Mode.AddPolygon;
        if (ImEditorGui.selectableIcon("Add Node##TSE_TCSE_Collision_Add_Node_Mode_Selectable", EditorIcons.Icons.Add, "Click to start adding polygon", isAddMode, modeSelectableSize, modeSelectableSize)) {
            editingMode = isAddMode ? null : Mode.AddPolygon;
        }

        ImGui.tableNextColumn();
        boolean isEditMode = editingMode == Mode.EditPolygon;
        if (ImEditorGui.selectableIcon("Edit Node##TSE_TCSE_Collision_Edit_Node_Mode_Selectable", EditorIcons.Icons.Edit, "Click to start editing polygon", isEditMode, modeSelectableSize, modeSelectableSize)) {
            editingMode = isEditMode ? null : Mode.EditPolygon;
        }

        ImGui.tableNextColumn();
        boolean isRemoveMode = editingMode == Mode.RemovePolygon;
        if (ImEditorGui.selectableIcon("Remove Node##TSE_TCSE_Collision_Remove_Node_Mode_Selectable", EditorIcons.Icons.Remove, "CLick start removing polygon", isRemoveMode, modeSelectableSize, modeSelectableSize)) {
            editingMode = isRemoveMode ? null : Mode.RemovePolygon;
        }

        ImGui.tableNextColumn();
        if (ImEditorGui.iconButton("Reset Node##TSE_TCSE_Collision_Reset_Node_Button", EditorIcons.Icons.Reset, "Click to reset polygon to tile shape")) resetToTileShape(tile);
        
        ImGui.endTable();
        ImGui.separator();
        ImGui.spacing();
        renderTilePreview(tileset.getTextureID(), tile, tileset.getGridSize());
        ImGui.spacing();
        ImGui.indent();
        boolean polyOpen = ImGui.collapsingHeader("Polygon##TSE_TCSE_Collision_Node_Coordinates_Header_");
        if (!polyOpen) {
            ImGui.unindent();
            return;
        }

        if (tile.collisionPolygonNodes == null || tile.collisionPolygonNodes.length == 0) {
            ImGui.beginDisabled();
            ImGui.textWrapped("This tile has no collision shape");
            ImGui.endDisabled();
            ImGui.unindent();
            return;
        }

        Vector2f[] nodes = tile.collisionPolygonNodes;
        for (Vector2f node : nodes) {
            Vector2f tmp = new Vector2f(node);
            ImEditorGui.dragVec2Ctrl("", tmp, 0.0f, 0.0f, 0.01f, node, 0.0f, 1.0f);
            if (!tmp.equals(node)) node.set(Math.max(0.0f, Math.min(1.0f, tmp.x)), Math.max(0.0f, Math.min(1.0f, tmp.y)));
        }
    }

    private static void renderTilePreview(int textureID, Tile tile, Vector2i gridSize) {
        if (textureID <= -1 || gridSize == null || gridSize.x < 0 || gridSize.y < 0) return;
        Vector2f[] coordinates = tile.textureCoordinates;
        if (coordinates == null) return;

        ImGui.beginGroup();
        Vector2f scaledSize = TextureScale.calculateFitSquare(gridSize.x, gridSize.y, tilePreviewSize);
        float remainWidth = ImGui.getContentRegionAvailX();
        float offset = Math.max((remainWidth - tilePreviewSize) * 0.5f, 0.0f);
        float normalX = ImGui.getCursorPosX();
        ImGui.setCursorPosX(normalX + offset);
        ImVec2 cursorPos = ImGui.getCursorPos();
        drawTileBackground(scaledSize);
        ImGui.image(textureID, scaledSize.x, scaledSize.y, coordinates[2].x, coordinates[0].y, coordinates[0].x, coordinates[2].y);
        ImGui.setCursorPos(cursorPos);
        drawPolygonShape(tile, scaledSize);
        ImGui.setCursorPos(cursorPos);
        handleEditMode(scaledSize, tile);
        ImGui.endGroup();
    }

    private static void resetToTileShape(Tile tile) {
        if (tile == null) return;
        tile.collisionPolygonNodes = new Vector2f[] {
                new Vector2f(),
                new Vector2f(1.0f, 0f),
                new Vector2f(1.0f),
                new Vector2f(0.0f, 1.0f)
        };
    }

    private static void drawTileBackground(Vector2f size) {
        ImDrawList drawList = ImGui.getWindowDrawList();

        ImVec2 screenCursorPos = ImGui.getCursorScreenPos();
        ImVec2 rectMin = new ImVec2(screenCursorPos);
        ImVec2 rectMax = new ImVec2(screenCursorPos.x + size.x, screenCursorPos.y + size.y);

        drawList.addRectFilled(rectMin, rectMax, tileBackgroundFillColor);
        drawList.addRect(rectMin, rectMax, tileBackgroundBorderColor);
    }

    private static void drawPolygonShape(Tile tile, Vector2f size) {
        if (tile == null || size == null) return;
        Vector2f[] coordinates = tile.collisionPolygonNodes;
        if (coordinates == null || coordinates.length < 3) return;
        ImDrawList drawList = ImGui.getWindowDrawList();
        ImVec2 screenCursorPos = ImGui.getCursorScreenPos();

        List<ImVec2> screenNodes = new ArrayList<>();
        for (Vector2f node : coordinates) {
            float x = screenCursorPos.x + node.x * size.x;
            float y = screenCursorPos.y + (1.0f - node.y) * size.y;
            screenNodes.add(new ImVec2(x, y));
        }

        if (screenNodes.size() >= 3) {
            ImVec2[] points = screenNodes.toArray(new ImVec2[0]);
            drawList.addConvexPolyFilled(points, points.length, PolygonFillColor);
        }

        for (int i = 0; i < screenNodes.size(); i++) {
            ImVec2 start = screenNodes.get(i);
            ImVec2 end = screenNodes.get((i + 1) % screenNodes.size());
            drawList.addLine(start, end, PolygonColor, 2f);
        }

        ImVec2 mousePos = ImGui.getMousePos();
        for (ImVec2 node : screenNodes) {
            float dx = mousePos.x - node.x;
            float dy = mousePos.y - node.y;
            float dSqr = dx * dx + dy * dy;
            boolean isHover = dSqr < NodeRadius * NodeRadius;

            drawList.addCircleFilled(node, NodeRadius, isHover ? NodeHoverColor : NodeColor);
        }
    }

    private static void handleEditMode(Vector2f size, Tile tile) {
        if (size == null || tile == null) return;
        if (editingMode != Mode.EditPolygon || tile.collisionPolygonNodes == null || tile.collisionPolygonNodes.length == 0) return;
        ImVec2 screenCursorPos = ImGui.getCursorScreenPos();
        ImVec2 mousePos = ImGui.getMousePos();
        boolean isMouseDown = ImGui.isMouseDown(GLFW_MOUSE_BUTTON_1);
        boolean isMouseClick = ImGui.isMouseClicked(GLFW_MOUSE_BUTTON_1);

        boolean onPreview = mousePos.x >= screenCursorPos.x - NodeRadius && mousePos.x <= screenCursorPos.x + size.x + NodeRadius
                && mousePos.y >= screenCursorPos.y - NodeRadius && mousePos.y <= screenCursorPos.y + size.y + NodeRadius;
        if (!onPreview) return;

        Vector2f[] nodes = tile.collisionPolygonNodes;
        if (isMouseClick) {
            for (int i = 0; i < nodes.length; i++) {
                float x = screenCursorPos.x + nodes[i].x * size.x;
                float y = screenCursorPos.y + (1.0f - nodes[i].y) * size.y;

                float dx = mousePos.x - x;
                float dy = mousePos.y - y;
                float dSqr = dx * dx + dy * dy;
                if (Math.abs(dSqr - NodeRadius * NodeRadius) <= 0.1f) System.out.println("Not tolerated margin!");
                if (dSqr > NodeRadius * NodeRadius) continue;
                draggingNodeIndex = i;
                break;
            }
        }

        if (isMouseDown && draggingNodeIndex >= 0 && draggingNodeIndex < nodes.length) {
            float normalX = (mousePos.x - screenCursorPos.x) / size.x;
            float normalY = 1.0f - (mousePos.y - screenCursorPos.y) / size.y;
            normalX = Math.max(0.0f, Math.min(1.0f, normalX));
            normalY = Math.max(0.0f, Math.min(1.0f, normalY));
            nodes[draggingNodeIndex].set(normalX, normalY);
        }

        if (!isMouseDown) draggingNodeIndex = -1;
    }
}

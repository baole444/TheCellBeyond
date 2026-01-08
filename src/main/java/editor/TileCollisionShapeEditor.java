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
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_2;

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
    private static final float EdgeHoverThreshold = 6.0f;
    private static final float ModeSelectableSize = 20.0f;

    private static final int TileBackgroundBorderColor = ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1.0f);
    private static final int TileBackgroundFillColor = ImGui.getColorU32(0.1f, 0.1f, 0.1f, 1.0f);
    private static final int NodeColor = ImGui.getColorU32(0.7f, 0.7f, 0.7f, 1.0f);
    private static final int NodeHoverColor = ImGui.getColorU32(0.9f, 0.9f, 0.75f, 1.0f);
    private static final int PolygonColor = ImGui.getColorU32(0.8f, 0.1f, 0.1f, 0.7f);
    private static final int PolygonFillColor = ImGui.getColorU32(0.8f, 0.1f, 0.1f, 0.4f);

    private static final int PendingNodeColor = ImGui.getColorU32(0.3f, 0.8f, 04f, 0.8f);
    private static final int PendingLineColor = ImGui.getColorU32(0.3f, 0.8f, 0.3f, 0.8f);
    private static final int PendingCloseLineColor = ImGui.getColorU32(0.8f, 0.8f, 0.3f, 0.6f);
    private static final int EdgeHoverColor = ImGui.getColorU32(0.3f, 0.8f, 0.8f, 1.0f);

    private static Tile editingTile = null;
    private static Mode editingMode = null;
    private static int draggingNodeIndex = -1;

    private static int hoveringEdge = -1;
    private static boolean draggingNewNode = false;
    private static final List<Vector2f> pendingNodes = new ArrayList<>();

    private static void resetData() {
        editingMode = null;
        editingTile = null;
        draggingNodeIndex = -1;
        hoveringEdge = -1;
        pendingNodes.clear();
        draggingNewNode = false;
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
        if (ImEditorGui.selectableIcon("Add Node##TSE_TCSE_Collision_Add_Node_Mode_Selectable", EditorIcons.Icons.Add, "Click to start adding polygon", isAddMode, ModeSelectableSize, ModeSelectableSize)) {
            if (isAddMode) {
                if (!pendingNodes.isEmpty() && pendingNodes.size() < MinNode) pendingNodes.clear();
                draggingNewNode = false;
                editingMode = null;
            } else {
                editingMode = Mode.AddPolygon;
                pendingNodes.clear();
                draggingNewNode = false;
            }
        }

        ImGui.tableNextColumn();
        boolean isEditMode = editingMode == Mode.EditPolygon;
        if (ImEditorGui.selectableIcon("Edit Node##TSE_TCSE_Collision_Edit_Node_Mode_Selectable", EditorIcons.Icons.Edit, "Click to start editing polygon", isEditMode, ModeSelectableSize, ModeSelectableSize)) {
            editingMode = isEditMode ? null : Mode.EditPolygon;
            pendingNodes.clear();
            draggingNewNode = false;
        }

        ImGui.tableNextColumn();
        boolean isRemoveMode = editingMode == Mode.RemovePolygon;
        if (ImEditorGui.selectableIcon("Remove Node##TSE_TCSE_Collision_Remove_Node_Mode_Selectable", EditorIcons.Icons.Remove, "Click start removing polygon", isRemoveMode, ModeSelectableSize, ModeSelectableSize)) {
            editingMode = isRemoveMode ? null : Mode.RemovePolygon;
            pendingNodes.clear();
            draggingNewNode = false;
        }

        ImGui.tableNextColumn();
        if (ImEditorGui.iconButton("Reset Node##TSE_TCSE_Collision_Reset_Node_Button", EditorIcons.Icons.Reset, "Click to reset polygon to tile shape")) {
            pendingNodes.clear();
            hoveringEdge = -1;
            draggingNewNode = false;
            resetToTileShape(tile);
        }
        
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
        drawPendingNodes(scaledSize);
        ImGui.setCursorPos(cursorPos);
        handleAddMode(scaledSize, tile);
        handleEditMode(scaledSize, tile);
        handleRemoveNode(scaledSize, tile);
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

        drawList.addRectFilled(rectMin, rectMax, TileBackgroundFillColor);
        drawList.addRect(rectMin, rectMax, TileBackgroundBorderColor);
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
            drawList.addConcavePolyFilled(points, points.length, PolygonFillColor);
        }

        for (int i = 0; i < screenNodes.size(); i++) {
            ImVec2 start = screenNodes.get(i);
            ImVec2 end = screenNodes.get((i + 1) % screenNodes.size());

            boolean isHovered = (editingMode == Mode.AddPolygon && hoveringEdge == i);
            int edgeColor = isHovered ? EdgeHoverColor : PolygonColor;
            float edgeThickness = isHovered ? 2.4f : 2.0f;
            drawList.addLine(start, end, edgeColor, edgeThickness);
        }

        ImVec2 mousePos = ImGui.getMousePos();
        for (ImVec2 node : screenNodes) {
            boolean isHover = inNodeRadius(node, mousePos);
            drawList.addCircleFilled(node, NodeRadius, isHover ? NodeHoverColor : NodeColor);
        }
    }

    private static void drawPendingNodes(Vector2f size) {
        if (editingMode != Mode.AddPolygon || pendingNodes.isEmpty()) return;

        ImDrawList drawList = ImGui.getWindowDrawList();
        ImVec2 screenCursorPos = ImGui.getCursorScreenPos();
        ImVec2 mousePos = ImGui.getMousePos();

        List<ImVec2> screenNodes = new ArrayList<>();
        for (Vector2f node : pendingNodes) {
            float x = screenCursorPos.x + node.x * size.x;
            float y = screenCursorPos.y + (1.0f - node.y) * size.y;
            screenNodes.add(new ImVec2(x, y));
        }

        for (int i = 0; i < screenNodes.size() - 1; i++) {
            drawList.addLine(screenNodes.get(i), screenNodes.get(i + 1), PendingLineColor, 2.0f);
        }

        if (screenNodes.size() >= MinNode) {
            ImVec2 first = screenNodes.getFirst();
            ImVec2 last = screenNodes.getLast();
            if (inNodeRadius(first, mousePos)) drawList.addLine(last, first, PendingCloseLineColor, 2.0f);
        }

        for (ImVec2 node : screenNodes) {
            drawList.addCircleFilled(node, NodeRadius, PendingNodeColor);
        }
    }

    private static void handleAddMode(Vector2f size, Tile tile) {
        if (size == null || tile == null || editingMode != Mode.AddPolygon) return;
        ImVec2 screenCursorPos = ImGui.getCursorScreenPos();
        ImVec2 mousePos = ImGui.getMousePos();
        boolean isMouseClick = ImGui.isMouseClicked(GLFW_MOUSE_BUTTON_1);
        if (!onPreview(size, mousePos, screenCursorPos)) {
            hoveringEdge = -1;
            return;
        }

        if (tile.collisionPolygonNodes == null || tile.collisionPolygonNodes.length == 0) handleAddNewPolygon(size, screenCursorPos, mousePos, isMouseClick);
        else handleInsertPolygon(size, tile, screenCursorPos, mousePos, isMouseClick);
    }

    private static void handleEditMode(Vector2f size, Tile tile) {
        if (size == null || tile == null) return;
        if (editingMode != Mode.EditPolygon || tile.collisionPolygonNodes == null || tile.collisionPolygonNodes.length == 0) return;
        ImVec2 screenCursorPos = ImGui.getCursorScreenPos();
        ImVec2 mousePos = ImGui.getMousePos();
        boolean isMouseDown = ImGui.isMouseDown(GLFW_MOUSE_BUTTON_1);
        boolean isMouseClick = ImGui.isMouseClicked(GLFW_MOUSE_BUTTON_1);
        if (!onPreview(size, mousePos, screenCursorPos)) return;

        Vector2f[] nodes = tile.collisionPolygonNodes;
        if (isMouseClick) {
            for (int i = 0; i < nodes.length; i++) {
                if (!inNodeRadius(size, screenCursorPos, mousePos, nodes[i])) continue;
                draggingNodeIndex = i;
                break;
            }
        }

        if (isMouseDown && draggingNodeIndex >= 0 && draggingNodeIndex < nodes.length) {
            nodes[draggingNodeIndex].set(getNormal(mousePos, screenCursorPos, size));
        }

        if (!isMouseDown) draggingNodeIndex = -1;
    }

    private static void handleRemoveNode(Vector2f size, Tile tile) {
        if (size == null || tile == null) return;
        if (editingMode != Mode.RemovePolygon || tile.collisionPolygonNodes == null || tile.collisionPolygonNodes.length == 0) return;

        ImVec2 screenCursorPos = ImGui.getCursorScreenPos();
        ImVec2 mousePos = ImGui.getMousePos();
        boolean leftClick = ImGui.isMouseClicked(GLFW_MOUSE_BUTTON_1);
        boolean rightClick = ImGui.isMouseClicked(GLFW_MOUSE_BUTTON_2);

        if (!onPreview(size, mousePos, screenCursorPos) || (!leftClick && !rightClick)) return;

        Vector2f[] nodes = tile.collisionPolygonNodes;
        for (int i = 0; i < nodes.length; i++) {
            if (!inNodeRadius(size, screenCursorPos, mousePos, nodes[i])) continue;
            if (rightClick) {
                tile.collisionPolygonNodes = null;
                return;
            }

            if (nodes.length <= MinNode) {
                tile.collisionPolygonNodes = null;
                return;
            }

            Vector2f[] newNodes = new Vector2f[nodes.length - 1];
            int index = 0;
            for (int j = 0; j < nodes.length; j++) {
                if (j == i) continue;
                newNodes[index++] = nodes[j];
            }
            tile.collisionPolygonNodes = newNodes;
            return;
        }
    }

    private static void handleAddNewPolygon(Vector2f size, ImVec2 screenCursorPos, ImVec2 mousePos, boolean isMouseClick) {
        boolean isMouseDown = ImGui.isMouseDown(GLFW_MOUSE_BUTTON_1);
        boolean isMouseRelease = ImGui.isMouseReleased(GLFW_MOUSE_BUTTON_1);
        Vector2f normal = getNormal(mousePos, screenCursorPos, size);
        if (isMouseClick) {
            if (closingPolygon(size, screenCursorPos, mousePos)) return;
            if (pendingNodes.size() >= MaxNode) return;
            draggingNewNode = true;
            pendingNodes.add(normal);
        }

        if (draggingNewNode && isMouseDown && !pendingNodes.isEmpty()) pendingNodes.getLast().set(normal);
        if (isMouseRelease && draggingNewNode) draggingNewNode = false;
    }

    private static boolean closingPolygon(Vector2f size, ImVec2 screenCursorPos, ImVec2 mousePos) {
        if (pendingNodes.size() < MinNode) return false;
        Vector2f firstNode = pendingNodes.getFirst();
        if (!inNodeRadius(size, screenCursorPos, mousePos, firstNode)) return false;
        addPendingNodes();
        draggingNewNode = false;
        return true;
    }

    private static void addPendingNodes() {
        if (editingTile == null || pendingNodes.size() < MinNode) return;
        editingTile.collisionPolygonNodes = pendingNodes.toArray(Vector2f[]::new);
        pendingNodes.clear();
    }

    private static void handleInsertPolygon(Vector2f size, Tile tile, ImVec2 screenCursorPos, ImVec2 mousePos, boolean isMouseClick) {
        Vector2f[] nodes = tile.collisionPolygonNodes;
        if (nodes.length >= MaxNode) {
            hoveringEdge = -1;
            return;
        }

        hoveringEdge = -1;
        float minDist = EdgeHoverThreshold;
        int nearestEdge = -1;
        for (int i = 0; i < nodes.length; i++) {
            Vector2f v1 = nodes[i];
            Vector2f v2 = nodes[(i + 1) % nodes.length];
            ImVec2 start = new ImVec2(screenCursorPos.x + v1.x * size.x, screenCursorPos.y + (1.0f - v1.y) * size.y) ;
            ImVec2 end = new ImVec2(screenCursorPos.x + v2.x * size.x, screenCursorPos.y + (1.0f - v2.y) * size.y);
            float distSqr = distanceSquareToSegment(mousePos, start, end);
            if (distSqr >= minDist * minDist) continue;
            minDist = (float) Math.sqrt(distSqr);
            nearestEdge = i;
        }

        hoveringEdge = nearestEdge;
        if (isMouseClick && hoveringEdge >= 0) insertNodeToEdge(tile, size, screenCursorPos, mousePos, hoveringEdge);
    }

    private static void insertNodeToEdge(Tile tile, Vector2f size, ImVec2 screenCursorPos, ImVec2 mousePos, int edgeIndex) {
        Vector2f[] currentNodes = tile.collisionPolygonNodes;

        Vector2f normal = getNormal(mousePos, screenCursorPos, size);
        Vector2f start = currentNodes[edgeIndex];
        Vector2f end = currentNodes[(edgeIndex + 1) % currentNodes.length];
        Vector2f projected = projectOnEdge(normal, start, end);
        Vector2f[] newNodes = new Vector2f[currentNodes.length + 1];
        System.arraycopy(currentNodes, 0, newNodes, 0, edgeIndex + 1);
        newNodes[edgeIndex + 1] = projected;
        if (currentNodes.length - (edgeIndex + 1) >= 0) {
            System.arraycopy(currentNodes, edgeIndex + 1, newNodes, edgeIndex + 1 + 1, currentNodes.length - (edgeIndex + 1));
        }
        tile.collisionPolygonNodes = newNodes;
        hoveringEdge = -1;
    }

    private static Vector2f projectOnEdge(Vector2f point, Vector2f start, Vector2f end) {
        Vector2f segment = new Vector2f(end).sub(start);
        Vector2f p = new Vector2f(point).sub(start);
        float lengthSqr = segment.lengthSquared();
        if (lengthSqr == 0) return new Vector2f(start);
        float t = p.dot(segment) / lengthSqr;
        t = Math.max(0.0f, Math.min(1.0f, t));
        return new Vector2f(start).add(new Vector2f(segment).mul(t));
    }

    private static float distanceSquareToSegment(ImVec2 mousePos, ImVec2 start, ImVec2 end) {
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float px = mousePos.x - start.x;
        float py = mousePos.y - start.y;

        if (dx == 0.0f && dy == 0.0f) return px * px + py * py;

        float t = (px * dx + py * dy) / (dx * dx + dy * dy);
        t = Math.max(0.0f, Math.min(1.0f, t));
        float nearX = start.x + t * dx;
        float nearY = start.y + t * dy;
        px = mousePos.x - nearX;
        py = mousePos.y - nearY;

        return px * px + py * py;
    }

    private static Vector2f getNormal(ImVec2 mousePos, ImVec2 screenCursorPos, Vector2f size) {
        float normalX = (mousePos.x - screenCursorPos.x) / size.x;
        float normalY = 1.0f - (mousePos.y - screenCursorPos.y) / size.y;
        normalX = Math.max(0.0f, Math.min(1.0f, normalX));
        normalY = Math.max(0.0f, Math.min(1.0f, normalY));
        return new Vector2f(normalX, normalY);
    }

    private static boolean onPreview(Vector2f size, ImVec2 mousePos, ImVec2 screenCursorPos) {
        return mousePos.x >= screenCursorPos.x - NodeRadius && mousePos.x <= screenCursorPos.x + size.x + NodeRadius
                && mousePos.y >= screenCursorPos.y - NodeRadius && mousePos.y <= screenCursorPos.y + size.y + NodeRadius;
    }

    private static boolean inNodeRadius(ImVec2 screenNode, ImVec2 mousePos) {
        float dx = mousePos.x - screenNode.x;
        float dy = mousePos.y - screenNode.y;
        float dSqr = dx * dx + dy * dy;
        return dSqr < NodeRadius * NodeRadius;
    }

    private static boolean inNodeRadius(Vector2f size, ImVec2 screenCursorPos, ImVec2 mousePos, Vector2f nodes) {
        float x = screenCursorPos.x + nodes.x * size.x;
        float y = screenCursorPos.y + (1.0f - nodes.y) * size.y;

        float dx = mousePos.x - x;
        float dy = mousePos.y - y;
        float dSqr = dx * dx + dy * dy;
        return dSqr <= NodeRadius * NodeRadius;
    }
}

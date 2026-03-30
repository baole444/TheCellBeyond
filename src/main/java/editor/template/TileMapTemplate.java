package editor.template;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.TileMap;
import editor.EditorIcons;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import org.joml.Vector2i;
import render.texture.TileSet;

import java.util.UUID;

/**
 * Template for {@link TileMap}'s editor UI.
 */
final class TileMapTemplate implements ObjectTemplate<TileMap> {
    private static final TileMapTemplate instance = new TileMapTemplate();
    private TileMapTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     *
     * @param object the context object
     */
    @Override
    public void editorUI(TileMap object) {
        ImGui.spacing();
        UUID uuid = object.getUUID();
        boolean openMap = ImGui.collapsingHeader("TileMap##TileMap_Properties_Header_" + uuid, ImGuiTreeNodeFlags.DefaultOpen);
        if (!openMap) return;
        ImBoolean enableCollision = new ImBoolean(object.enableCollision);
        if (ImGui.checkbox("Enable Collision##TileMap_Enable_Collision_" + uuid, enableCollision)) object.enableCollision = enableCollision.get();
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text("enableCollision = " + object.enableCollision);
            ImGui.separator();
            ImGui.spacing();
            ImGui.text("If false, all collision for this tile map will be disabled.");
            ImGui.endTooltip();
        }
        ImGui.spacing();
        ImBoolean useKinematicBody = new ImBoolean(object.useKinematicBody);
        if (ImGui.checkbox("Use kinematic body##TileMap_Physic_Use_Kinematic_Body_" + uuid, useKinematicBody)) object.useKinematicBody = useKinematicBody.get();
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text("useKinematicBody = " + object.useKinematicBody);
            ImGui.separator();
            ImGui.spacing();
            ImGui.text("If true, physic body of this tile map will be of kinematic type.");
            ImGui.textWrapped("Can be used for creating moving platform.");
            ImGui.endTooltip();
        }
        ImGui.indent();
        boolean openSet = false;
        if (ImGui.beginTable("##TileSet_TileMap_Properties_Layout" + uuid, 3, ImGuiTableColumnFlags.WidthFixed)) {
            ImGui.tableSetupColumn("##TileSet_TileMap_Header_Column_" + uuid, ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableSetupColumn("##TileSet_TileMap_Reset_Column_" + uuid, ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableSetupColumn("##TileSet_TileMap_Delete_Column_" + uuid, ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableNextColumn();
            ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
            openSet = ImGui.collapsingHeader("TileSet##TileMap_TileSet_Properties_Header_" + uuid, ImGuiTreeNodeFlags.DefaultOpen);
            ImGui.popStyleColor(1);
            ImGui.tableNextColumn();
            if (EditorWidget.iconButton("TileSet_TileMap_Reset_Button_" + uuid, EditorIcons.Icons.Reset, "Reset this tile set to default")) object.resetToDefault();
            ImGui.tableNextColumn();
            if (EditorWidget.iconButton("TileSet_TileMap_Delete_Button_" + uuid, EditorIcons.Icons.Delete, "Delete this tile set")) object.deleteTileSet();
            ImGui.endTable();
        }
        if (!openSet) {
            ImGui.unindent();
            return;
        }
        TileSet tileSet = object.tileSet();
        if (tileSet == null) {
            if (ImGui.button("Create new Tile set", ImGui.getContentRegionAvailX(), 0.0f)) object.tileSet(new TileSet());
            ImGui.unindent();
            return;
        }
        ImGui.text("Tile size:");
        Vector2i size = tileSet.gridSize();
        int x = EditorWidget.dragIntCtrl("With", size.x, 16, tileSet, 1);
        int y = EditorWidget.dragIntCtrl("Height", size.y, 16, tileSet, 1);
        if (x != size.x || y != size.y) tileSet.gridSize(size.set(x, y));
        ImGui.separator();
        ImGui.text("Start position offset:");
        Vector2i startOffset = tileSet.startPosition();
        int xF = EditorWidget.dragIntCtrl("X offset", startOffset.x, 0, tileSet, 0);
        int yF = EditorWidget.dragIntCtrl("Y offset", startOffset.y, 0, tileSet, 0);
        if (xF != startOffset.x || yF != startOffset.y) tileSet.startPosition(startOffset.set(xF, yF));
        ImGui.separator();
        ImGui.text("Physic layers");
        ImGui.spacing();
        ImGui.indent();
        int collisionLayer = EditorWidget.physicLayerSelectable("Collision Layer", tileSet.getCollisionLayer(), object);
        ImGui.spacing();
        int collisionMask = EditorWidget.physicLayerSelectable("Collision Mask", tileSet.getCollisionMask(), object);
        tileSet.setCollisionLayer(collisionLayer);
        tileSet.setCollisionMask(collisionMask);
        ImGui.unindent();
        ImGui.separator();
        ImGui.spacing();
        ImGui.unindent();
    }

    /**
     * Render the content of {@link #editorUI(TileMap)} and call {@link GameObject2DTemplate#render(GameObject2D)}.
     * @param tileMap the context object
     */
    static void render(TileMap tileMap) {
        if (tileMap == null) return;
        instance.editorUI(tileMap);
        GameObject2DTemplate.render(tileMap);
    }
}

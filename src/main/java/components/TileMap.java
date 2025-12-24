package components;

import editor.EditorIcons;
import editor.ImEditorGui;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.texture.Tile;
import render.texture.TileSet;
import utility.WorldUnit;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class TileMap extends SpatialComponent {
    private volatile TileSet tileSet;
    private final ConcurrentHashMap<Vector2i, Tile> tiles = new ConcurrentHashMap<>();
    public boolean enableCollision = true;
    public boolean useKinematicBody = false;

    private volatile transient boolean isTileDirty = true;

    @Override
    protected void additionalDirtyFlagLogic() {
        isTileDirty = true;
    }

    public boolean isTileDirty() {
        if (tileSet != null && tileSet.requestRendererUpdate()) isTileDirty = true;
        return isTileDirty;
    }

    public void setTileDirty(boolean needsUpdate) {
        isTileDirty = needsUpdate;
        if (!needsUpdate && tileSet != null) tileSet.rendererUpdated();
    }

    public Vector2f getTileSetSize() {
        if (tileSet == null) return new Vector2f(1.0f);

        return new Vector2f(tileSet.getWidth(), tileSet.getHeight());
    }

    public Vector2f getSpriteSizeAsWorldUnit() {
        return WorldUnit.pixelToWorld(getTileSetSize());
    }

    public HashMap<Vector2i, Tile> getTiles() {
        return new HashMap<>(tiles);
    }

    public TileSet getTileSet() {
        return tileSet;
    }

    public void setTileSet(TileSet tileSet) {
        if (Objects.equals(this.tileSet, tileSet)) return;
        this.tileSet = tileSet;
        tiles.clear();
        isTileDirty = true;
    }

    public void placeTile(Vector2i mapCoordinate, Vector2i tileSetCoordinate) {
        if (tileSet == null || mapCoordinate == null || tileSetCoordinate == null) return;

        Tile tile = tileSet.getTile(tileSetCoordinate);
        if (tile == null) return;

        tiles.put(mapCoordinate, tile);

        isTileDirty = true;
    }

    public void placeTiles(Vector2i startingMapCoordinate, List<Vector2i> tileSetCoordinates) {
        if (tileSet == null || startingMapCoordinate == null || tileSetCoordinates == null || tileSetCoordinates.isEmpty()) return;

        Vector2i firstCoordinate = tileSetCoordinates.getFirst();
        if (firstCoordinate == null) return;

        for (Vector2i grid : tileSetCoordinates) {
            if (grid == null) continue;
            Tile tile = tileSet.getTile(grid);
            if (tile == null) continue;
            Vector2i offset = new Vector2i(grid.x - firstCoordinate.x, firstCoordinate.y - grid.y);

            tiles.put(new Vector2i(startingMapCoordinate).add(offset), tile);
        }

        isTileDirty = true;
    }

    public boolean removeTile(Vector2i mapCoordinate) {
        if (mapCoordinate == null) return false;

        Tile tile = tiles.remove(mapCoordinate);
        boolean removed = tile != null;
        if (removed) isTileDirty = true;
        return removed;
    }

    public void deleteTileSet() {
        tiles.clear();
        tileSet = null;
        isTileDirty = true;
    }

    public void resetToDefault() {
        if (tileSet == null) return;
        tiles.clear();
        tileSet.resetDefault();
        isTileDirty = true;
    }

    @Override
    protected void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openMap = ImGui.collapsingHeader("TileMap##TileMap_Properties_Header_" + getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openMap) return;

        ImBoolean enableCollision = new ImBoolean(this.enableCollision);
        if (ImGui.checkbox("Enable Collision##TileMap_Enable_Collision_" + getUUID(), enableCollision)) this.enableCollision = enableCollision.get();
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text("enableCollision = " + this.enableCollision);
            ImGui.separator();
            ImGui.spacing();
            ImGui.text("If false, all collision for this tile map will be disabled.");
            ImGui.endTooltip();
        }
        ImGui.spacing();
        ImBoolean useKinematicBody = new ImBoolean(this.useKinematicBody);
        if (ImGui.checkbox("Use kinematic body##TileMap_Physic_Use_Kinematic_Body_" + getUUID(), useKinematicBody)) this.useKinematicBody = useKinematicBody.get();
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text("useKinematicBody = " + this.useKinematicBody);
            ImGui.separator();
            ImGui.spacing();
            ImGui.text("If true, physic body of this tile map will be of kinematic type.");
            ImGui.textWrapped("Can be used for creating moving platform.");
            ImGui.endTooltip();
        }

        ImGui.indent();
        boolean openSet = false;
        if (ImGui.beginTable("##TileSet_TileMap_Properties_Layout" + getUUID(), 3, ImGuiTableColumnFlags.WidthFixed)) {
            ImGui.tableSetupColumn("##TileSet_TileMap_Header_Column_" + getUUID(), ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableSetupColumn("##TileSet_TileMap_Reset_Column_" + getUUID(), ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableSetupColumn("##TileSet_TileMap_Delete_Column_" + getUUID(), ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableNextColumn();
            ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
            openSet = ImGui.collapsingHeader("TileSet##TileMap_TileSet_Properties_Header_" + getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
            ImGui.popStyleColor(1);
            ImGui.tableNextColumn();
            if (ImEditorGui.iconButton("TileSet_TileMap_Reset_Button_" + getUUID(), EditorIcons.Icons.Reset, "Reset this tile set to default")) resetToDefault();
            ImGui.tableNextColumn();
            if (ImEditorGui.iconButton("TileSet_TileMap_Delete_Button_" + getUUID(), EditorIcons.Icons.Delete, "Delete this tile set")) deleteTileSet();
            ImGui.endTable();
        }
        if (!openSet) {
            ImGui.unindent();
            return;
        }

        if (tileSet == null) {
            if (ImGui.button("Create new Tile set", ImGui.getContentRegionAvailX(), 0.0f)) setTileSet(new TileSet());
            ImGui.unindent();
            return;
        }

        ImGui.text("Tile size:");
        Vector2i size = tileSet.getGridSize();
        int x = ImEditorGui.dragIntCtrl("With", size.x, 16, tileSet, 1);
        int y = ImEditorGui.dragIntCtrl("Height", size.y, 16, tileSet, 1);
        if (x != size.x || y != size.y) tileSet.setGridSize(size.set(x, y));

        ImGui.separator();
        ImGui.text("Start position offset:");
        Vector2i startOffset = tileSet.getStartPosition();
        int xF = ImEditorGui.dragIntCtrl("X offset", startOffset.x, 0, tileSet, 0);
        int yF = ImEditorGui.dragIntCtrl("Y offset", startOffset.y, 0, tileSet, 0);
        if (xF != startOffset.x || yF != startOffset.y) tileSet.setStartPosition(startOffset.set(xF, yF));

        ImGui.separator();
        ImGui.text("Physic layers");
        ImGui.spacing();
        ImGui.indent();
        int collisionLayer = ImEditorGui.physicLayerSelectable("Collision Layer", tileSet.getCollisionLayer(), this);
        ImGui.spacing();
        int collisionMask = ImEditorGui.physicLayerSelectable("Collision Mask", tileSet.getCollisionMask(), this);
        tileSet.setCollisionLayer(collisionLayer);
        tileSet.setCollisionMask(collisionMask);
        ImGui.unindent();
        ImGui.separator();
        ImGui.spacing();
        ImGui.unindent();
    }
}

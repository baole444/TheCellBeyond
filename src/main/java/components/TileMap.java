package components;

import imgui.ImGui;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.texture.Tile;
import render.texture.TileSet;
import utility.WorldUnit;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class TileMap extends SpatialComponent {
    private volatile TileSet tileSet;
    private final ConcurrentHashMap<Vector2i, Tile> tiles = new ConcurrentHashMap<>();

    private volatile transient boolean isTileDirty = true;

    @Override
    protected void additionalImGuiLogic() {
        ImGui.textDisabled("More control later");
    }

    @Override
    protected void additionalDirtyFlagLogic() {
        isTileDirty = true;
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
            Vector2i offset = new Vector2i(grid).sub(firstCoordinate);

            tiles.put(new Vector2i(startingMapCoordinate).add(offset), tile);
        }

        isTileDirty = true;
    }
}

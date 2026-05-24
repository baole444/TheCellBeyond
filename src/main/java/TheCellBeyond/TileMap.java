package TheCellBeyond;

import TheCellBeyond.internal.LogicServer;
import components.IsNotSelectable;
import org.joml.Vector2f;
import org.joml.Vector2i;
import physic2d.KinematicBody2D;
import physic2d.PhysicBody2D;
import physic2d.StaticBody2D;
import physic2d.collider.TileCollider2D;
import render.commands.MeshCommand;
import render.commands.RenderCommand;
import render.texture.Tile;
import render.texture.TileSet;
import scene.Scene;
import utility.WorldUnit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class TileMap extends GameObject2D {
    public record TilePlacement(Vector2i sourceCoordinate, int quarterRotations, boolean flipVertical, boolean flipHorizontal) {
        public TilePlacement {
            if (sourceCoordinate == null) sourceCoordinate = new Vector2i();
            else sourceCoordinate = new Vector2i(sourceCoordinate);
            quarterRotations = Math.clamp(quarterRotations, -4, 4);
        }

        public TilePlacement(Vector2i sourceCoordinate) {
            this (sourceCoordinate, 0, false, false);
        }
    }

    private volatile TileSet tileSet;
    private final ConcurrentHashMap<Vector2i, TilePlacement> tiles = new ConcurrentHashMap<>();
    public boolean enableCollision = true;
    public boolean useKinematicBody = false;
    private transient PhysicBody2D physicBody2D = null;
    private transient boolean physicBodyDirty = false;

    public TileMap() {
        String name = TileMap.class.getSimpleName();
        this(name);
    }

    public TileMap(String name) {
        if (invalidName(name)) name = TileMap.class.getSimpleName();
        super(name);
    }

    @Override
    protected void internalStart() {
        initPhysicBody();
    }

    @Override
    protected void internalDestroy() {
        if (physicBody2D == null) return;
        physicBody2D.destroy();
        physicBody2D = null;
    }

    private void initPhysicBody() {
        if (!enableCollision) return;
        if (physicBody2D != null) return;

        String physicName = "TileMap_PhysicBody_" + getUUID().toString();
        physicBody2D = useKinematicBody ? new KinematicBody2D(physicName) : new StaticBody2D(physicName);
        physicBody2D.setNotSerialize();
        physicBody2D.addComponents(new IsNotSelectable(), new TileCollider2D());
        physicBody2D.friction(1.0f);
        if (tileSet != null) {
            physicBody2D.setCollisionLayer(tileSet.getCollisionLayer());
            physicBody2D.setCollisionMask(tileSet.getCollisionMask());
        }
        addChild(physicBody2D);
        Scene scene = LogicServer.currentScene();
        if (scene != null) scene.queueForObjectAddition(physicBody2D, this);
    }

    public PhysicBody2D physicBody2D() {
        return physicBody2D;
    }

    public boolean isTileDirty() {
        if (tileSet != null && tileSet.requestRendererUpdate()) renderDirty = true;
        return renderDirty;
    }

    public void setTileDirty(boolean needsUpdate) {
        renderDirty = needsUpdate;
        if (!needsUpdate && tileSet != null) tileSet.rendererUpdated();
    }

    public Vector2f getTileSetSize() {
        if (tileSet == null) return new Vector2f(1.0f);
        return new Vector2f(tileSet.width(), tileSet.height());
    }

    public Vector2f getSpriteSizeAsWorldUnit() {
        return WorldUnit.pixelToWorld(getTileSetSize());
    }

    public HashMap<Vector2i, Tile> tiles() {
        if (tileSet == null || tiles.isEmpty()) return new HashMap<>();
        HashMap<Vector2i, Tile> result = new HashMap<>();
        for (Map.Entry<Vector2i, TilePlacement> entry : tiles.entrySet()) {
            Tile tile = tileSet.tile(entry.getValue().sourceCoordinate);
            if (tile != null) result.put(entry.getKey(), tile);
        }
        return result;
    }

    public HashMap<Vector2i, TilePlacement> tilePlacements() {
        return new HashMap<>(tiles);
    }

    public TileSet tileSet() {
        return tileSet;
    }

    public void tileSet(TileSet tileSet) {
        if (Objects.equals(this.tileSet, tileSet)) return;
        this.tileSet = tileSet;
        tiles.clear();
        renderDirty = true;
    }

    public void placeTile(Vector2i mapCoordinate, Vector2i tileSetCoordinate) {
        placeTile(mapCoordinate, tileSetCoordinate, 0, false, false);
    }

    public void placeTile(Vector2i mapCoordinate, Vector2i tileSetCoordinate, int quarterRotations, boolean flipVertical, boolean flipHorizontal) {
        if (tileSet == null || mapCoordinate == null || tileSetCoordinate == null) return;
        Tile tile = tileSet.tile(tileSetCoordinate);
        if (tile == null) return;
        tiles.put(mapCoordinate, new TilePlacement(tileSetCoordinate, quarterRotations, flipVertical, flipHorizontal));
        renderDirty = true;
    }

    public void placeTiles(Vector2i startingMapCoordinate, List<Vector2i> tileSetCoordinates) {
        if (tileSet == null || startingMapCoordinate == null || tileSetCoordinates == null || tileSetCoordinates.isEmpty()) return;
        Vector2i firstCoordinate = tileSetCoordinates.getFirst();
        if (firstCoordinate == null) return;
        for (Vector2i grid : tileSetCoordinates) {
            if (grid == null) continue;
            Tile tile = tileSet.tile(grid);
            if (tile == null) continue;
            Vector2i offset = new Vector2i(grid.x - firstCoordinate.x, firstCoordinate.y - grid.y);
            tiles.put(new Vector2i(startingMapCoordinate).add(offset), new TilePlacement(grid));
        }
        renderDirty = true;
    }

    public boolean removeTile(Vector2i mapCoordinate) {
        if (mapCoordinate == null) return false;
        TilePlacement placement = tiles.remove(mapCoordinate);
        boolean removed = placement != null;
        if (removed) renderDirty = true;
        return removed;
    }

    public void deleteTileSet() {
        tiles.clear();
        tileSet = null;
        renderDirty = true;
    }

    public void resetToDefault() {
        if (tileSet == null) return;
        tiles.clear();
        tileSet.resetDefault();
        renderDirty = true;
    }

    @Override
    public TileMap copy() {
        return copy(false);
    }

    @Override
    public TileMap copy(boolean copyHierarchy) {
        TileMap copy = (TileMap) copySingleObject();
        if (copyHierarchy && !getChildren().isEmpty()) copyDescendants(this, copy);
        return copy;
    }

    @Override
    public boolean renderDirty() {
        return isTileDirty();
    }

    @Override
    public void renderDirty(boolean dirty) {
        setTileDirty(dirty);
    }

    @Override
    public RenderCommand buildRenderCommand() {
        MeshCommand command = MeshCommand.acquire();
        command.submitterID = getUID();
        command.tilePlacements = tilePlacements();
        command.tileSet = tileSet;
        command.markChanged();
        return command;
    }
}

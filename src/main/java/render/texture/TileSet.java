package render.texture;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TileSet contains the grid size of tile, start position offset and a hash map of computed
 * coordinates for each tile.<br>
 * TileSet itself also contains a Sprite of which hold the texture of this set.<br>
 * Similar to Sprite, TileSet will mark itself dirty on parameter update ot it's sprite is dirty.
 * If the sprite that it is holding is null, TileSet will disable its dirty flag.
 */
public class TileSet {
    private final Vector2i gridSize;
    private final Vector2i startPosition;
    private Sprite tileSetSprite;
    private final ConcurrentHashMap<Vector2i, Vector2f[]> tiles = new ConcurrentHashMap<>();

    private volatile transient boolean tileDirty = true;

    public TileSet(Vector2i gridSize, Vector2i startPosition, Sprite tileSetSprite) {
        this.gridSize = new Vector2i(gridSize);
        this.startPosition = new Vector2i(startPosition);
        this.tileSetSprite = tileSetSprite;
    }

    public void setTileSetSprite(Sprite sprite) {
        if (Objects.equals(tileSetSprite, sprite)) return;
        tileSetSprite = sprite;

        if (sprite == null) {
            tiles.clear();
            tileDirty = false;
            return;
        }

        HashMap<Vector2i, Vector2f[]> updates = new HashMap<>(tiles);
        for (Map.Entry<Vector2i, Vector2f[]> entry : updates.entrySet()) {
            Vector2i position = entry.getKey();
            Vector2f[] newCoordinates = calculateTileTextureCoordinate(position);
            if (newCoordinates == null) newCoordinates = deadTileCoordinates();

            tiles.put(position, newCoordinates);
        }

        tileDirty = true;
    }

    public void addTile(Vector2i gridPosition) {
        if (gridPosition == null || gridPosition.x < 0 || gridPosition.y < 0 || tiles.containsKey(gridPosition)) return;

        Vector2f[] coordinates = calculateTileTextureCoordinate(gridPosition);
        if (coordinates == null) {
            System.err.println("Tile's coordinate for (" + gridPosition.x + "," + gridPosition.y + ") is dead, no tile was added.");
            return;
        }

        tiles.put(gridPosition, coordinates);

        tileDirty = true;
    }

    public void removeTile(Vector2i gridPosition) {
        if (gridPosition == null || gridPosition.x < 0 || gridPosition.y < 0 || !tiles.containsKey(gridPosition)) return;

        tiles.remove(gridPosition);

        tileDirty = true;
    }

    public float getWidth() {
        return tileSetSprite != null ? tileSetSprite.getWidth() : 0.0f;
    }

    public float getHeight() {
        return tileSetSprite != null ? tileSetSprite.getHeight() : 0.0f;
    }

    public int getTextureID() {
        if (tileSetSprite == null) return -1;

        return tileSetSprite.getTextureID();
    }

    public Vector2f[] getTileCoordinate(Vector2i gridPosition) {
        if (gridPosition == null || gridPosition.x < 0 || gridPosition.y < 0 || !tiles.containsKey(gridPosition)) return deadTileCoordinates();

        return tiles.get(gridPosition);
    }

    public List<Vector2f[]> getTileCoordinates() {
        if (tiles.isEmpty()) return List.of();

        return tiles.values().stream().toList();
    }

    public void rendererUpdated() {
        if (tileSetSprite != null) tileSetSprite.rendererUpdated();

        tileDirty = false;
    }

    public boolean requestRendererUpdate() {
        if (tileSetSprite == null) return false;

        return tileSetSprite.requestRendererUpdate() || tileDirty;
    }

    private Vector2f[] calculateTileTextureCoordinate(Vector2i gridPosition) {
        if (tileSetSprite == null) return null;
        if (gridPosition.x < 0 || gridPosition.y < 0 || gridSize.x < 0 || gridSize.y < 0) return null;
        if (tileSetSprite.getWidth() <= 0.0f || tileSetSprite.getHeight() <= 0.0f) return null;

        int x = startPosition.x + (gridPosition.x * gridSize.x);
        int y = startPosition.y + (gridPosition.y * gridSize.y);

        float textureW = tileSetSprite.getWidth();
        float textureH = tileSetSprite.getHeight();

        int instY = (int) (textureH - y - gridSize.y);

        float leftX = x / textureW;
        float rightX = (x + gridSize.x) / textureW;
        float bottomY = instY / textureH;
        float topY = (instY + gridSize.y) / textureH;

        return new Vector2f[] {
                new Vector2f(rightX, topY),
                new Vector2f(rightX, bottomY),
                new Vector2f(leftX, bottomY),
                new Vector2f(leftX, topY)
        };
    }

    private static Vector2f[] deadTileCoordinates() {
        return new Vector2f[] {
                new Vector2f(0.0f),
                new Vector2f(0.0f),
                new Vector2f(0.0f),
                new Vector2f(0.0f)
        };
    }
}

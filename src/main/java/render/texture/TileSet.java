package render.texture;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.HashMap;

public class TileSet {
    private final Vector2i gridSize;
    private final Vector2i startPosition;
    private final Sprite tileSetSprite = new Sprite();
    private final Vector2f[] textureCoordinateBlueprint = {
            new Vector2f(1, 1),
            new Vector2f(1, 0),
            new Vector2f(0, 0),
            new Vector2f(0, 1)
    };

    private final HashMap<Vector2i, Vector2f[]> tiles = new HashMap<>();

    public TileSet(Vector2i gridSize, Vector2i startPosition, String textureCanonicalPath) {
        this.gridSize = new Vector2i(gridSize);
        this.startPosition = new Vector2i(startPosition);
        tileSetSprite.setTexture(textureCanonicalPath);
    }

    private Vector2f[] calculateTileTextureCoordinate(Vector2i gridPosition) {
        if (gridPosition.x < 0 || gridPosition.y < 0) return null;
        if (gridSize.x < 0 || gridSize.y < 0) return null;
        if ( tileSetSprite.getWidth() <= 0.0f || tileSetSprite.getHeight() <= 0.0f) return null;

        return new Vector2f[] {};
    }
}

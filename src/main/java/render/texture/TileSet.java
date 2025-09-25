package render.texture;

import org.joml.Vector2i;

import java.util.HashMap;
import java.util.Map;

public class TileSet {
    private Vector2i gridSize;
    private Vector2i startPosition;
    private Sprite tileSetSprite;

    private final Map<Vector2i, Tile> tiles = new HashMap<>();
}

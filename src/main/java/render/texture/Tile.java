package render.texture;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Tile {
    public Vector2i setCoordinate;
    public Vector2f[] textureCoordinates;
    /**
     * A hash map of collision nodes per collision layer.
     */
    public final ConcurrentHashMap<Integer, Vector2f[]> collisionShapeNode = new ConcurrentHashMap<>();
}

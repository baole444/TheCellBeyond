package render.texture;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.List;

public class Tile {
    public Vector2i setCoordinate;
    public Vector2f[] textureCoordinates;
    public List<Vector2f> collisionShapeCoordinates;
}

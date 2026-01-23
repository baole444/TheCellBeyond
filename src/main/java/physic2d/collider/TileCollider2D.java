package physic2d.collider;

import TheCellBeyond.TileMap;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.texture.Tile;
import render.texture.TileSet;
import utility.WorldUnit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TileCollider2D extends CollisionShape2D {
    public static final int MinNode = 3;
    public static final int MaxNode = 8;

    private TileMap tileMap;
    private final Set<Shape> shapes = new HashSet<>();

    @Override
    protected void onStart() {
        if (gameObject != null && gameObject.getParent() instanceof TileMap map) tileMap = map;
        super.onStart();
    }

    @Override
    protected void onEditorStart() {
        if (gameObject != null && gameObject.getParent() instanceof TileMap map) tileMap = map;
        super.onEditorStart();
    }

    @Override
    public Shape createCollisionShape() {
        return null;
    }

    @Override
    protected void drawDebugShape() {}

    public Set<Shape> createCollisionShapes() {
        shapes.clear();
        if (tileMap == null || tileMap.tileSet() == null) return shapes;

        HashMap<Vector2i, TileMap.TilePlacement> placements = tileMap.tilePlacements();
        if (placements.isEmpty()) return shapes;

        TileSet tileSet = tileMap.tileSet();
        Vector2i gridSize = tileMap.tileSet().gridSize();
        Vector2f gridWorldSize = WorldUnit.pixelToWorld(gridSize.x, gridSize.y);

        for (Map.Entry<Vector2i, TileMap.TilePlacement> entry : placements.entrySet()) {
            Vector2i mapCoordinate = entry.getKey();
            TileMap.TilePlacement placement = entry.getValue();
            Tile tile = tileSet.tile(placement.sourceCoordinate());
            if (!hasCollisionShape(tile)) continue;

            Vector2f tileWorldPosition = tileWorldPosition(mapCoordinate, gridWorldSize);
            Vec2[] worldNodes = toWorldNodes(tile.collisionPolygonNodes, tileWorldPosition, gridWorldSize);
            if (worldNodes.length < MinNode) continue;

            Shape shape = createShapeFromNodeArray(worldNodes);
            if (shape != null) shapes.add(shape);
        }

        return shapes;
    }

    private static boolean hasCollisionShape(Tile tile) {
        if (tile == null || tile.collisionPolygonNodes == null) return false;
        return tile.collisionPolygonNodes.length >= MinNode;
    }

    private static Vector2f tileWorldPosition(Vector2i mapCoordinate, Vector2f gridWorldSize) {
        return new Vector2f(mapCoordinate).mul(gridWorldSize);
    }

    private static Vec2[] toWorldNodes(Vector2f[] normalizedNodes, Vector2f tilePosition, Vector2f tileSize) {
        int count = Math.min(normalizedNodes.length, MaxNode);
        Vec2[] worldNodes = new Vec2[count];

        for (int i = 0; i < count; i++) {
            Vector2f coordinate = normalizedNodes[i];
            worldNodes[i] = new Vec2(
                    tilePosition.x + coordinate.x * tileSize.x,
                    tilePosition.y + coordinate.y * tileSize.y
            );
        }

        return worldNodes;
    }
}

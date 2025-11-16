package components;

import TheCellBeyond.Viewport;
import TheCellBeyond.Window;
import editor.TileMapEditor;
import editor.project.Project;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import render.DebugDraw;
import render.texture.TileSet;
import utility.WorldUnit;

public class TileMapGrid extends Component implements  NotSerializeComponent {
    private static final Vector4f gridColor = new Vector4f(0.85f, 0.4f, 0.1f, 0.5f);

    public static volatile boolean draw = false;

    @Override
    public void editorUpdate(float dt) {
        if (!draw) return;

        TileMap editingTileMap = TileMapEditor.getEditingTileMap();
        if (editingTileMap == null) return;

        TileSet tileSet = editingTileMap.getTileSet();
        if (tileSet == null) return;

        Vector2i gridSize = tileSet.getGridSize();
        if (gridSize.x <= 0 || gridSize.y <= 0) return;
        Vector2f gridWorldSize = WorldUnit.pixelToWorld(new Vector2f(gridSize.x, gridSize.y));

        Viewport viewport = Window.getScene().viewport();
        float totalZoom = viewport.getZoom() / Project.preference().textureGlobalScale();
        Vector2f viewPos = viewport.position;
        Vector2f projectSize = viewport.getProjectionSize();

        Vector2f tileMapPos = editingTileMap.getPosition();

        float firstX = (int) Math.floor((viewPos.x - tileMapPos.x) / gridWorldSize.x) * gridWorldSize.x + tileMapPos.x;
        float firstY = (int) Math.floor((viewPos.y - tileMapPos.y) / gridWorldSize.y) * gridWorldSize.y + tileMapPos.y;
        float width = (int) (projectSize.x * totalZoom) + gridWorldSize.x * 5;
        float height = (int) (projectSize.y * totalZoom) + gridWorldSize.y * 5;

        int countVertical = (int) (projectSize.x * totalZoom / gridWorldSize.x) + 2;
        int countHorizontal = (int) (projectSize.y * totalZoom / gridWorldSize.y) + 2;
        int maxLines = Math.max(countVertical, countHorizontal);

        for (int i = 0; i < maxLines; i++) {
            float x = firstX + gridWorldSize.x * i;
            float y = firstY + gridWorldSize.y * i;
            DebugDraw.addLine2(new Vector2f(x, firstY), new Vector2f(x, firstY + height), gridColor);
            DebugDraw.addLine2(new Vector2f(firstX, y), new Vector2f(firstX + width, y), gridColor);
        }
    }
}

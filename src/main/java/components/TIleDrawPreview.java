package components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.MouseListener;
import TheCellBeyond.Window;
import editor.TileMapEditor;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import render.texture.Sprite;
import render.texture.Tile;
import render.texture.TileSet;
import utility.WorldUnit;

import java.util.ArrayList;
import java.util.List;

public class TIleDrawPreview extends Component implements NotSerializeComponent {
    private static final Vector4f previewColor = new Vector4f(1.0f, 1.0f, 1.0f, 0.35f);

    private GameObject2D holdingObj = null;
    private Vector2i lastGridPosition = null;
    private List<Tile> lastSelectedTiles = null;

    @Override
    public void editorUpdate(float dt) {
        TileMap editingTileMap = TileMapEditor.getEditingTileMap();

        if (editingTileMap == null || !isDrawMode()) {
            clearData();
            return;
        }

        TileSet tileSet = editingTileMap.getTileSet();
        if (tileSet == null) {
            clearData();
            return;
        }

        List<Tile> selectedTiles = TileMapEditor.getSelectedTiles();
        if (selectedTiles.isEmpty()) {
            clearData();
            return;
        }

        Vector2i gridPos = calculateGridPos(editingTileMap, tileSet);
        if (gridPos == null) {
            clearData();
            return;
        }

        if (needUpdate(selectedTiles)) {
            updateHoldingObj(editingTileMap, tileSet, gridPos, selectedTiles);
        }

        updatePosition(editingTileMap, tileSet, gridPos);
    }

    private boolean needUpdate(List<Tile> selectedTiles) {
        if (lastSelectedTiles == null || lastSelectedTiles.size() != selectedTiles.size()) return true;

        for (int i = 0; i < selectedTiles.size(); i++) {
            if (lastSelectedTiles.get(i) != selectedTiles.get(i)) return true;
        }

        return false;
    }

    private void updateHoldingObj(TileMap tileMap, TileSet tileSet, Vector2i gridPos, List<Tile> selectedTiles) {
        clearData();
        if (selectedTiles.isEmpty()) return;

        Vector2f mapPos = tileMap.getPosition();
        Vector2i gridSize = tileSet.getGridSize();
        Vector2f gridWorldSize = WorldUnit.pixelToWorld(new Vector2f(gridSize.x, gridSize.y));

        Tile firstTile = selectedTiles.getFirst();
        Vector2i firstCoordinate = firstTile.setCoordinate;
        if (firstCoordinate == null) return;

        float anchorWorldX = mapPos.x + gridPos.x * gridWorldSize.x;
        float anchorWorldY = mapPos.y + gridPos.y * gridWorldSize.y;

        GameObject2D tileObject = new GameObject2D("EditorTilesObject");
        tileObject.setPosition(anchorWorldX, anchorWorldY);
        tileObject.addComponent(new IsNotSelectable());
        tileObject.setNotSerialize();

        for (Tile tile : selectedTiles) {
            if (tile == null || tile.setCoordinate == null) continue;
            Vector2i offset = new Vector2i(tile.setCoordinate).sub(firstCoordinate);
            SpriteRenderer spriteRenderer = new SpriteRenderer();
            Sprite sprite = new Sprite();
            sprite.setTexture(tileSet.getTexture());
            sprite.setTextureCoordinates(tile.textureCoordinates);
            sprite.setWidth(gridSize.x);
            sprite.setHeight(gridSize.y);
            spriteRenderer.setSprite(sprite);
            spriteRenderer.setColor(previewColor);

            Vector2f localOffset = new Vector2f(offset.x * gridWorldSize.x, -offset.y * gridWorldSize.y);
            spriteRenderer.setLocalPosition(localOffset);

            tileObject.addComponent(spriteRenderer);
        }

        Window.getScene().queueForObjectAddition(tileObject);
        holdingObj = tileObject;

        lastGridPosition = new Vector2i(gridPos);
        lastSelectedTiles = new ArrayList<>(selectedTiles);
    }

    private void updatePosition(TileMap tileMap, TileSet tileSet, Vector2i gridPos) {
        if (lastGridPosition != null && lastGridPosition.equals(gridPos)) return;
        if (holdingObj == null || holdingObj.isRemoved()) return;

        Vector2f mapPos = tileMap.getPosition();
        Vector2i gridSize = tileSet.getGridSize();
        Vector2f gridWorldSize = WorldUnit.pixelToWorld(new Vector2f(gridSize.x, gridSize.y));

        float anchorWorldX = mapPos.x + gridPos.x * gridWorldSize.x + gridWorldSize.x / 2.0f;
        float anchorWorldY = mapPos.y + gridPos.y * gridWorldSize.y + gridWorldSize.y / 2.0f;

        holdingObj.setPosition(anchorWorldX, anchorWorldY);
        lastGridPosition = new Vector2i(gridPos);
    }


    private Vector2i calculateGridPos(TileMap tileMap, TileSet tileSet) {
        Vector2f mapPos = tileMap.getPosition();
        Vector2i gridSize = tileSet.getGridSize();

        if (gridSize.x <= 0 || gridSize.y <= 0) return null;

        float width = WorldUnit.pixelToWorld(gridSize.x);
        float height = WorldUnit.pixelToWorld(gridSize.y);

        float mouseX = MouseListener.getWorldX();
        float mouseY = MouseListener.getWorldY();

        float relativeX = mouseX - mapPos.x;
        float relativeY = mouseY - mapPos.y;

        int x = (int) Math.floor(relativeX / width);
        int y = (int) Math.floor(relativeY / height);

        return new Vector2i(x, y);
    }

    private boolean isDrawMode() {
        return TileMapGrid.draw;
    }

    private void clearData() {
        if (holdingObj != null && !holdingObj.isRemoved()) {
            holdingObj.destroy();
        }

        holdingObj = null;
        lastGridPosition = null;
        lastSelectedTiles = null;
    }

    @Override
    protected void additionalDestroyLogic() {
        clearData();
    }


}

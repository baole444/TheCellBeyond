package components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.KeyListener;
import TheCellBeyond.MouseListener;
import TheCellBeyond.Window;
import editor.ImGuiLayer;
import editor.TileMapEditor;
import imgui.ImGui;
import imgui.flag.ImGuiPopupFlags;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import render.DebugDraw;
import render.texture.Sprite;
import render.texture.Tile;
import render.texture.TileSet;
import utility.WorldUnit;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public class TileMapCtrl extends Component implements NotSerializeComponent {
    private static final Vector4f previewColor = new Vector4f(1.0f, 1.0f, 1.0f, 0.35f);
    private static final Vector4f eraserColor = new Vector4f(1.0f, 0.25f, 0.25f, 0.75f);

    private GameObject2D holdingObj = null;
    private Vector2i lastGridPosition = null;
    private List<Tile> lastSelectedTiles = null;
    private transient boolean cleared = true;

    @Override
    public void editorUpdate(float dt) {
        if (!ImGuiLayer.getWantedCaptureMouse() || ImGui.isPopupOpen("", ImGuiPopupFlags.AnyPopup)) return;

        TileMap editingTileMap = TileMapEditor.getEditingTileMap();

        if (editingTileMap == null) {
            clearData();
            return;
        }

        TileSet tileSet = editingTileMap.getTileSet();
        if (tileSet == null) {
            clearData();
            return;
        }

        Vector2i gridPos = calculateGridPos(editingTileMap, tileSet);
        if (gridPos == null) {
            clearData();
            return;
        }

        if (TileMapEditor.isDrawMode()) {
            List<Tile> selectedTiles = TileMapEditor.getSelectedTiles();
            if (selectedTiles.isEmpty()) {
                if (KeyListener.isKeyTapped(GLFW_KEY_ESCAPE)) TileMapEditor.escapeMode();
                clearData();
                return;
            }

            if (needUpdate(selectedTiles)) {
                updateHoldingObj(editingTileMap, tileSet, gridPos, selectedTiles);
            }

            updatePosition(editingTileMap, tileSet, gridPos);
            handleDrawInput(editingTileMap, gridPos, selectedTiles);
            return;
        }

        if (TileMapEditor.isEraseMode()) {
            if (holdingObj != null) clearData();
            drawEraserSquare(editingTileMap, tileSet, gridPos);
            handleEraseInput(editingTileMap, gridPos);
        }
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

        float anchorWorldX = mapPos.x + gridPos.x * gridWorldSize.x + gridWorldSize.x / 2.0f;
        float anchorWorldY = mapPos.y + gridPos.y * gridWorldSize.y + gridWorldSize.y / 2.0f;

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
        cleared = false;
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

    private void drawEraserSquare(TileMap tileMap, TileSet tileSet, Vector2i gridPos) {
        Vector2f mapPos = tileMap.getPosition();
        Vector2i gridSize = tileSet.getGridSize();
        Vector2f gridWorldSize = WorldUnit.pixelToWorld(new Vector2f(gridSize.x, gridSize.y));

        float anchorWorldX = mapPos.x + gridPos.x * gridWorldSize.x;
        float anchorWorldY = mapPos.y + gridPos.y * gridWorldSize.y;
        Vector2f centre = new Vector2f(anchorWorldX + gridWorldSize.x / 2.0f, anchorWorldY + gridWorldSize.y / 2.0f);

        DebugDraw.addBox2(centre, gridWorldSize, 0.0f, eraserColor);
        DebugDraw.addLine2(new Vector2f(anchorWorldX, anchorWorldY), new Vector2f(anchorWorldX + gridWorldSize.x, anchorWorldY + gridWorldSize.y), eraserColor);
        DebugDraw.addLine2(new Vector2f(anchorWorldX, anchorWorldY + gridWorldSize.y), new Vector2f(anchorWorldX + gridWorldSize.x, anchorWorldY), eraserColor);
    }

    private void handleDrawInput(TileMap tileMap, Vector2i gridPos, List<Tile> selectedTiles) {
        if (KeyListener.isKeyTapped(GLFW_KEY_ESCAPE)) {
            TileMapEditor.clearSelectedTiles();
            clearData();
            return;
        }

        if (selectedTiles.isEmpty()) return;
        if (!MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_1)) return;

        if (selectedTiles.size() == 1) {
            Tile tile = selectedTiles.getFirst();
            if (tile == null || tile.setCoordinate == null) return;
            tileMap.placeTile(gridPos, tile.setCoordinate);
            return;
        }

        List<Vector2i> tileSetCoordinates = new ArrayList<>();
        for (Tile tile : selectedTiles) {
            if (tile == null || tile.setCoordinate == null) continue;
            tileSetCoordinates.add(tile.setCoordinate);
        }

        if (tileSetCoordinates.isEmpty()) return;
        tileMap.placeTiles(gridPos, tileSetCoordinates);
    }

    private void handleEraseInput(TileMap tileMap, Vector2i gridPos) {
        if (KeyListener.isKeyTapped(GLFW_KEY_ESCAPE)) {
            TileMapEditor.escapeMode();
            return;
        }

        if (!MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_1)) return;
        if (lastGridPosition != null && lastGridPosition.equals(gridPos)) return;

        boolean removed = tileMap.removeTile(gridPos);
        if (removed) {
            lastGridPosition = new Vector2i(gridPos);
            cleared = false;
        }
    }

    private Vector2i calculateGridPos(TileMap tileMap, TileSet tileSet) {
        Vector2f mapPos = tileMap.getPosition();
        Vector2i gridSize = tileSet.getGridSize();

        if (gridSize.x <= 0 || gridSize.y <= 0) return null;

        float width = WorldUnit.pixelToWorld(gridSize.x);
        float height = WorldUnit.pixelToWorld(gridSize.y);

        float mouseX = MouseListener.getWorldPositionX();
        float mouseY = MouseListener.getWorldPositionY();

        float relativeX = mouseX - mapPos.x;
        float relativeY = mouseY - mapPos.y;

        int x = (int) Math.floor(relativeX / width);
        int y = (int) Math.floor(relativeY / height);

        return new Vector2i(x, y);
    }

    private void clearData() {
        if (cleared) return;

        if (holdingObj != null && !holdingObj.isRemoved()) {
            holdingObj.destroy();
        }

        holdingObj = null;
        lastGridPosition = null;
        lastSelectedTiles = null;

        cleared = true;
    }

    @Override
    protected void additionalDestroyLogic() {
        clearData();
    }


}

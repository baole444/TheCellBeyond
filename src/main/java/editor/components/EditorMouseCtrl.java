package editor.components;

import TheCellBeyond.*;
import TheCellBeyond.internal.LogicServer;
import components.*;
import editor.*;
import editor.preference.UserPreference;
import imgui.ImGui;
import imgui.flag.ImGuiPopupFlags;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import render.DebugDraw;
import render.ObjectSelection;
import scene.Scene;
import utility.Settings;
import utility.WorldUnit;

import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A class dedicated to processing mouse's events for the editor.
 * Handle object's position and placement.
 */
public class EditorMouseCtrl extends Component implements NotSerializeComponent {
    private static final Vector4f resetColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Vector4f pickUpColor = new Vector4f(1f, 1f, 1f, 0.35f);

    GameObject holdObj = null;

    private boolean mouseButtonHeld = false;
    private final float clickResetTime = 0.2f;
    private float clickInit = clickResetTime;
    private boolean isBoxSelectionInit = false;

    private Vector2f boxSelectionBegin = new Vector2f();
    private Vector2f boxSelectionEnd = new Vector2f();

    public void pickObj(GameObject obj) {
        if (holdObj != null) holdObj.destroy();
        holdObj = obj;

        for (SpriteRenderer sprite : holdObj.getComponents(SpriteRenderer.class)) {
            sprite.setColor(pickUpColor);
        }
        holdObj.addComponent(new IsNotSelectable());
        holdObj.setNotSerialize();

        LogicServer.currentScene().queueForObjectAddition(obj);
    }

    public void placeObj() {
        GameObject newObj;
        newObj = holdObj.copy(true);

        for (SpriteRenderer sprite : newObj.getComponents(SpriteRenderer.class)) {
            sprite.setColor(resetColor);
        }

        newObj.removeComponents(IsNotSelectable.class);
        newObj.setSerialize(true);

        LogicServer.currentScene().queueForObjectAddition(newObj);
    }

    @Override
    public void editorUpdate(float dt) {
        if (EditorTileMapGrid.draw() && holdObj != null) {
            holdObj.destroy();
            holdObj = null;
        }

        clickInit -= dt;

        if (!ImGuiLayer.editorWantCaptureMouse() || ImGui.isPopupOpen("", ImGuiPopupFlags.AnyPopup)) return;

        if (holdObj == null) {
            onNotHoldingObject();
            return;
        }

        Vector2f targetPos = getTargetPos();
        if (holdObj instanceof GameObject2D go2D) {
            go2D.position(targetPos);
        } else if (holdObj instanceof GameObject go) {
            for (Component c : go.getComponents()) {
                if (c instanceof SpatialComponent sC) sC.setWorldPosition(targetPos);
            }
        }

        if (KeyListener.isKeyTapped(GLFW_KEY_ESCAPE)) {
            holdObj.destroy();
            holdObj = null;
            return;
        }

        if (!MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_LEFT)) {
            if (!MouseListener.isDragging() && !MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_LEFT) && mouseButtonHeld) {
                placeObj();
                mouseButtonHeld = false;
            }
            return;
        }

        float halfWidth = Settings.GRID_WIDTH / 2.0f;
        float halfHeight = Settings.GRID_HEIGHT / 2.0f;

        if (MouseListener.isDragging() && !isGridSquareOccupied(targetPos.x - halfWidth, targetPos.y - halfHeight)) {
            if (mouseButtonHeld) mouseButtonHeld = false;
            placeObj();
            return;
        }

        if (!MouseListener.isDragging() && clickInit < 0) {
            mouseButtonHeld = true;
            clickInit = clickResetTime;
        }
    }

    private static Vector2f getTargetPos() {
        float targetX, targetY;

        if (EditorTileMapGrid.draw()) {
            TileMap editingTileMap = TileMapEditor.getEditingTileMap();

            if (editingTileMap != null && editingTileMap.getTileSet() != null) {
                Vector2f tileMapPos = editingTileMap.globalPosition();
                Vector2i gridSize = editingTileMap.getTileSet().getGridSize();

                float gridWidth = WorldUnit.pixelToWorld(gridSize.x);
                float gridHeight = WorldUnit.pixelToWorld(gridSize.y);

                float mouseX = MouseListener.getWorldPositionX();
                float mouseY = MouseListener.getWorldPositionY();

                float relativeX = mouseX - tileMapPos.x;
                float relativeY = mouseY - tileMapPos.y;

                float gridX = Math.round(relativeX / gridWidth) * gridWidth;
                float gridY = Math.round(relativeY / gridHeight) * gridHeight;

                targetX = tileMapPos.x + gridX + gridWidth / 2.0f;
                targetY = tileMapPos.y + gridY + gridHeight / 2.0f;

                return new Vector2f(targetX, targetY);
            }
        }

        if (!UserPreference.editorPreferences().showGridLine()) {
            targetX = MouseListener.getWorldPositionX();
            targetY = MouseListener.getWorldPositionY();
        } else {
            float x = MouseListener.getWorldPositionX() - Settings.GRID_WIDTH / 2.0f;
            float y = MouseListener.getWorldPositionY() - Settings.GRID_HEIGHT / 2.0f;
            targetX = Math.round(x / Settings.GRID_WIDTH) * Settings.GRID_WIDTH + Settings.GRID_WIDTH / 2.0f;
            targetY = Math.round(y / Settings.GRID_HEIGHT) * Settings.GRID_HEIGHT + Settings.GRID_HEIGHT / 2.0f;
        }

        return new Vector2f(targetX, targetY);
    }

    private void onNotHoldingObject() {
        if (EditorTileMapGrid.draw()) return;

        ObjectSelection objectSelection = Window.getObjectSelection();
        Scene currentScene = LogicServer.currentScene();
        if (!MouseListener.isDragging() && MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_LEFT) && clickInit < 0) {
            int x = (int) MouseListener.getScreenPositionX();
            int y = (int) MouseListener.getScreenPositionY();

            int gObjectId = objectSelection.checkPixelAt(x, y);
            GameObject selectedObj = currentScene.getGameObject(gObjectId);

            if (selectedObj != null && selectedObj.getFirstComponent(IsNotSelectable.class) == null) {
                Properties.setActiveGameObject(selectedObj);
            } else if (selectedObj == null && !MouseListener.isDragging()) {
                Properties.clearSelection();
                SceneTree.clearSelection();
                BottomPanel.clear();
            }

            this.clickInit = clickResetTime;
            return;
        }

        if (MouseListener.isDragging() && MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_LEFT)) {
            if (!isBoxSelectionInit) {
                Properties.clearSelection();
                SceneTree.clearSelection();
                BottomPanel.clear();
                boxSelectionBegin = MouseListener.getScreenPosition();
                isBoxSelectionInit = true;
            }

            boxSelectionEnd = MouseListener.getScreenPosition();

            Vector2f boxSelectBeginWorld = MouseListener.screen2WorldCoordinate(boxSelectionBegin);
            Vector2f boxSelectEndWorld = MouseListener.screen2WorldCoordinate(boxSelectionEnd);

            Vector2f halfSize = (new Vector2f(boxSelectEndWorld).
                    sub(boxSelectBeginWorld)).mul(0.5f);

            DebugDraw.addBox2((new Vector2f(boxSelectBeginWorld)).add(halfSize),
                    new Vector2f(halfSize).mul(2.0f), 0.0f);

            return;
        }

        if (!isBoxSelectionInit) return;

        isBoxSelectionInit = false;
        int screenBeginX = (int) boxSelectionBegin.x;
        int screenBeginY = (int) boxSelectionBegin.y;
        int screenEndX = (int) boxSelectionEnd.x;
        int screenEndY = (int) boxSelectionEnd.y;
        boxSelectionBegin.zero();
        boxSelectionEnd.zero();

        if (screenEndX < screenBeginX) {
            int tmp = screenBeginX;
            screenBeginX = screenEndX;
            screenEndX = tmp;
        }

        if (screenEndY < screenBeginY) {
            int tmp = screenBeginY;
            screenBeginY = screenEndY;
            screenEndY = tmp;
        }

        float[] gameObjIds = objectSelection.checkPixelsIn(
                new Vector2i(screenBeginX, screenBeginY),
                new Vector2i(screenEndX, screenEndY)
        );

        Set<Integer> uniqueGOIds = new HashSet<>();

        for (float objId : gameObjIds) {
            uniqueGOIds.add((int) objId);
        }

        for (Integer objId : uniqueGOIds) {
            if (objId < 0) continue;

            GameObject selectedObj = LogicServer.currentScene().getGameObject(objId);
            if (selectedObj != null && selectedObj.getFirstComponent(IsNotSelectable.class) == null) Properties.addActiveGameObject(selectedObj);
        }
    }

    private boolean isGridSquareOccupied(float x, float y) {
        Vector2f begin = new Vector2f(x, y);
        Vector2f end = new Vector2f(begin).add(
                new Vector2f(Settings.GRID_WIDTH, Settings.GRID_HEIGHT)
        );

        Vector2f beginScrFloat = MouseListener.world2ScreenCoordinate(begin);
        Vector2f endScrFloat = MouseListener.world2ScreenCoordinate(end);

        // +- 2 to offset the coordinate inside the square border.
        Vector2i beginScr = new Vector2i((int)(beginScrFloat.x) + 2, (int)(beginScrFloat.y) + 2);
        Vector2i endScr = new Vector2i((int)(endScrFloat.x) - 2, (int)(endScrFloat.y) - 2);

        float[] gameObjIds = Window.getObjectSelection().checkPixelsIn(beginScr, endScr);

        for (float gameObjId : gameObjIds) {
            if (gameObjId <= 0) continue;

            GameObject selectedObj = LogicServer.currentScene().getGameObject((int) gameObjId);
            if (selectedObj.getFirstComponent(IsNotSelectable.class) == null) {
                return true;
            }
        }

        return false;
    }
}

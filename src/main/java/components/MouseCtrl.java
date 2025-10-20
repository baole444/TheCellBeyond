package components;

import TheCellBeyond.*;
import editor.ImGuiLayer;
import editor.Properties;
import editor.SceneTree;
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

import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A class dedicated to processing mouse's events for the editor.
 * Handle object's position and placement.
 */
public class MouseCtrl extends Component implements NotSerializeComponent {
    private static final Vector4f resetColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Vector4f pickUpColor = new Vector4f(1f, 1f, 1f, 0.35f);
    /**
     * A phantom object, allow preview of the real object's placement.
     * Is not serialized and is not selectable.
     */
    GameObject holdObj = null;

    /**
     * Click state registration
     */
    private boolean mouseButtonHeld = false;

    private final float clickResetTime = 0.2f;
    private float clickInit = clickResetTime;

    // check if dragging is already started
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

        Window.getScene().queueForObjectAddition(obj);
    }

    public void placeObj() {
        GameObject newObj;
        newObj = holdObj.copy(true);

        for (SpriteRenderer sprite : newObj.getComponents(SpriteRenderer.class)) {
            sprite.setColor(resetColor);
        }

        newObj.removeComponents(IsNotSelectable.class);
        newObj.setSerialize(true);

        Window.getScene().queueForObjectAddition(newObj);
    }

    @Override
    public void editorUpdate(float dt) {
        clickInit -= dt;

        if (holdObj == null) {
            onNotHoldingObject();
            return;
        }

        if (!ImGuiLayer.getWantedCaptureMouse() || ImGui.isPopupOpen("", ImGuiPopupFlags.AnyPopup)) return;

        Vector2f targetPos = getTargetPos();
        if (holdObj instanceof GameObject2D go2D) {
            go2D.setPosition(targetPos);
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

        if (!MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
            if (!MouseListener.isDragging() && !MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT) && mouseButtonHeld) {
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

        if (!UserPreference.editorPreferences().showGridLine()) {
            targetX = MouseListener.getWorldX();
            targetY = MouseListener.getWorldY();
        } else {
            float x = MouseListener.getWorldX() - Settings.GRID_WIDTH / 2.0f;
            float y = MouseListener.getWorldY() - Settings.GRID_HEIGHT / 2.0f;
            targetX = Math.round(x / Settings.GRID_WIDTH) * Settings.GRID_WIDTH + Settings.GRID_WIDTH / 2.0f;
            targetY = Math.round(y / Settings.GRID_HEIGHT) * Settings.GRID_HEIGHT + Settings.GRID_HEIGHT / 2.0f;
        }

        return new Vector2f(targetX, targetY);
    }

    private void onNotHoldingObject() {
        ObjectSelection objectSelection = Window.getObjectSelection();
        Scene currentScene = Window.getScene();
        if (!MouseListener.isDragging() && MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT) && clickInit < 0) {
            int x = (int) MouseListener.getScreenX();
            int y = (int) MouseListener.getScreenY();

            int gObjectId = objectSelection.checkPixelAt(x, y);
            GameObject selectedObj = currentScene.getGameObject(gObjectId);

            if (selectedObj != null && selectedObj.getFirstComponent(IsNotSelectable.class) == null) {
                Properties.setActiveGameObject(selectedObj);
            } else if (selectedObj == null && !MouseListener.isDragging()) {
                Properties.clearSelection();
                SceneTree.clearSelection();
            }

            this.clickInit = clickResetTime;
            return;
        }

        if (MouseListener.isDragging() && MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
            if (!isBoxSelectionInit) {
                Properties.clearSelection();
                SceneTree.clearSelection();
                boxSelectionBegin = MouseListener.getScreen();
                isBoxSelectionInit = true;
            }

            boxSelectionEnd = MouseListener.getScreen();

            Vector2f boxSelectBeginWorld = MouseListener.screen2WorldCoord(boxSelectionBegin);
            Vector2f boxSelectEndWorld = MouseListener.screen2WorldCoord(boxSelectionEnd);

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

            GameObject selectedObj = Window.getScene().getGameObject(objId);
            if (selectedObj != null && selectedObj.getFirstComponent(IsNotSelectable.class) == null) Properties.addActiveGameObject(selectedObj);
        }
    }

    private boolean isGridSquareOccupied(float x, float y) {
        Vector2f begin = new Vector2f(x, y);
        Vector2f end = new Vector2f(begin).add(
                new Vector2f(Settings.GRID_WIDTH, Settings.GRID_HEIGHT)
        );

        Vector2f beginScrFloat = MouseListener.world2ScreenCoord(begin);
        Vector2f endScrFloat = MouseListener.world2ScreenCoord(end);

        // +- 2 to offset the coordinate inside the square border.
        Vector2i beginScr = new Vector2i((int)(beginScrFloat.x) + 2, (int)(beginScrFloat.y) + 2);
        Vector2i endScr = new Vector2i((int)(endScrFloat.x) - 2, (int)(endScrFloat.y) - 2);

        float[] gameObjIds = Window.getObjectSelection().checkPixelsIn(beginScr, endScr);

        for (float gameObjId : gameObjIds) {
            if (gameObjId <= 0) continue;

            GameObject selectedObj = Window.getScene().getGameObject((int) gameObjId);
            if (selectedObj.getFirstComponent(IsNotSelectable.class) == null) {
                return true;
            }
        }

        return false;
    }
}

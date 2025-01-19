package components;

import TCB_Field.GameObject;
import TCB_Field.KeyListener;
import TCB_Field.MouseListener;
import TCB_Field.Window;
import editor.Properties;
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
public class MouseCtrl extends Component {
    /**
     * A phantom object, allow preview of the real object's placement.
     * Is not serialized and is not selectable.
     */
    GameObject holdObj = null;

    /**
     * Click state registration
     */
    private boolean mouseButtonHeld = false;

    // Debug value, checking holdObj with the last placed Object.
    // Investigating the issue where cancel placement (pressing escape)
    // will cause last placed object to disappear from the current scene.
    private GameObject lastPlaced = null;

    private float clickResetTime = 0.2f;
    private float clickInit = clickResetTime;

    // check if dragging is already started
    private boolean isBoxSelectionInit = false;

    private Vector2f boxSelectionBegin = new Vector2f();
    private Vector2f boxSelectionEnd = new Vector2f();

    public void pickObj(GameObject obj) {
        if (this.holdObj != null) {
            // Use to prevent placing an old object at the edge of viewport
            // When selecting new sprite to place.
            this.holdObj.destroy();
        }
        this.holdObj = obj;
        this.holdObj.getComponent(SpriteRender.class).setColor(new Vector4f(1f, 1f, 1f, 0.35f));
        this.holdObj.addComponent(new IsNotSelectable());

        // A fake object uses to illustrate targeted position (a preview).
        // It should not be savable ore appeared on the object grouping scene.
        this.holdObj.isNotSerialize();

        Window.getScene().addObjToScene(obj);
    }

    public void placeObj() {
        GameObject newObj = this.holdObj.copy();

        if (newObj.getComponent(StateEngine.class) != null) {
            newObj.getComponent(StateEngine.class).reloadTexture();
        }

        this.holdObj.transform.zIndex = 0;
        //this.holdObj.destroy();
        newObj.getComponent(SpriteRender.class).setColor(new Vector4f(1, 1, 1, 1));
        newObj.removeComponent(IsNotSelectable.class);

        // Make a placed object savable as it is now a real object.
        // A real object should be added to the object grouping scene.
        this.lastPlaced = newObj;
        newObj.isSerialize();

        System.out.println("Placing an object with uid: " + newObj.loadUid());
        Window.getScene().addObjToScene(newObj);
    }

    @Override
    public void editorUpdate(float dt) {
        clickInit -= dt;
        Properties properties = Window.loadImGui().loadProperties();
        ObjectSelection objectSelection = properties.loadObjSelection();
        Scene currentScene = Window.getScene();

        // Return coordinate base position from raw mouse input to place an active object in standard position.
        if (holdObj != null) {
            holdObj.transform.position.x = MouseListener.getWorldX() - Settings.GRID_WIDTH / 2.0f; // Might not need to - 0.16f for both
            holdObj.transform.position.y = MouseListener.getWorldY() - Settings.GRID_HEIGHT / 2.0f;

            holdObj.transform.position.x = Math.round(holdObj.transform.position.x / Settings.GRID_WIDTH) * Settings.GRID_WIDTH + Settings.GRID_WIDTH / 2.0f;
            holdObj.transform.position.y = Math.round(holdObj.transform.position.y / Settings.GRID_HEIGHT) * Settings.GRID_HEIGHT + Settings.GRID_HEIGHT / 2.0f;

            // When click released, place the object.
            if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
                float halfWidth = Settings.GRID_WIDTH / 2.0f;
                float halfHeight = Settings.GRID_HEIGHT / 2.0f;

                if (MouseListener.isDragging() &&
                        !isGridSquareOccupied(holdObj.transform.position.x - halfWidth,
                                holdObj.transform.position.y - halfHeight)) {

                    // Disable mouse register if performing dragging and placing.
                    // This is to prevent placing an additional object by check [1].
                    if (mouseButtonHeld) {
                        mouseButtonHeld = false;
                    }
                    placeObj();
                } else if (!MouseListener.isDragging() && clickInit < 0) {
                    // Set click register state to true.
                    mouseButtonHeld = true;

                    // When clicking but not dragging will place the object only once.
                    clickInit = clickResetTime;
                }

            }
            // [1]
            // Place an object when the mouse button is released.
            // The Object is placed on the next frame.
            else if (!MouseListener.isDragging() && !MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT) && mouseButtonHeld) {
                placeObj();

                // Reset click register state.
                mouseButtonHeld = false;
            }

            // Remove the current selected object to be place from the scene.
            if (KeyListener.isKeyPressed(GLFW_KEY_ESCAPE)) {
                //System.out.println("Holding obj: " + holdObj.loadUid());
                //System.out.println("Last placed object: " + lastPlaced.loadUid());
                this.holdObj.destroy();
                this.holdObj = null;
            }
        } else if (!MouseListener.isDragging() && MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT) && clickInit < 0) {
            int x = (int)MouseListener.getScreenX();
            int y = (int)MouseListener.getScreenY();

            int gObjectId = objectSelection.pixelCheck(x, y);
            GameObject selectedObj = currentScene.loadGameObj(gObjectId);

            // Excluding the gizmo
            if (selectedObj != null && selectedObj.getComponent(IsNotSelectable.class) == null) {
                properties.setActiveGameObj(selectedObj);
            } else if (selectedObj == null && !MouseListener.isDragging()) {
                properties.clearSelection();
            }

            this.clickInit = clickResetTime;
        } else if (MouseListener.isDragging() && MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
            if (!isBoxSelectionInit) {
                properties.clearSelection();
                boxSelectionBegin = MouseListener.getScreen();
                isBoxSelectionInit = true;
            }

            boxSelectionEnd = MouseListener.getScreen();

            Vector2f boxSelectBeginWorld = MouseListener.screen2WorldCoord(boxSelectionBegin);
            Vector2f boxSelectEndWorld = MouseListener.screen2WorldCoord(boxSelectionEnd);

            // get 1/2 dimension for selection box debug draw
            // because the box is drawn from the center.
            Vector2f halfSize = (new Vector2f(boxSelectEndWorld).
                    sub(boxSelectBeginWorld)).mul(0.5f);

            DebugDraw.addBox2((new Vector2f(boxSelectBeginWorld)).add(halfSize),
                    new Vector2f(halfSize).mul(2.0f), 0.0f);
        } else if (isBoxSelectionInit) {
            isBoxSelectionInit = false;

            // store box selection value temporary
            int screenBeginX = (int) boxSelectionBegin.x;
            int screenBeginY = (int) boxSelectionBegin.y;
            int screenEndX = (int) boxSelectionEnd.x;
            int screenEndY = (int) boxSelectionEnd.y;

            // reset value of box selection
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

            float[] gameObjIds = objectSelection.pixelCheck(
                    new Vector2i(screenBeginX, screenBeginY),
                    new Vector2i(screenEndX, screenEndY)
            );

            Set<Integer> uniqueGOIds = new HashSet<>();

            for (float objId : gameObjIds) {
                uniqueGOIds.add((int) objId);
            }

            for (Integer objId : uniqueGOIds) {
                GameObject selectedObj = Window.getScene().loadGameObj(objId);
                if (selectedObj != null && selectedObj.getComponent(IsNotSelectable.class) == null) {
                    properties.addActiveObj(selectedObj);
                }
            }
        }
    }

    private boolean isGridSquareOccupied(float x, float y) {
        Properties properties = Window.loadImGui().loadProperties();

        Vector2f begin = new Vector2f(x, y);
        Vector2f end = new Vector2f(begin).add(
                new Vector2f(Settings.GRID_WIDTH, Settings.GRID_HEIGHT)
        );

        Vector2f beginScrFloat = MouseListener.world2ScreenCoord(begin);
        Vector2f endScrFloat = MouseListener.world2ScreenCoord(end);

        // +- 2 to offset the coordinate inside the square border.
        Vector2i beginScr = new Vector2i((int)(beginScrFloat.x) + 2, (int)(beginScrFloat.y) + 2);
        Vector2i endScr = new Vector2i((int)(endScrFloat.x) - 2, (int)(endScrFloat.y) - 2);

        DebugDraw.addCircle(begin, 0.05f);
        DebugDraw.addCircle(end, 0.05f);

        float[] gameObjIds = properties.loadObjSelection().pixelCheck(beginScr, endScr);

        for (int i = 0; i < gameObjIds.length; i++) {
            if (gameObjIds[i] >= 0) {
                GameObject selectedObj = Window.getScene().loadGameObj((int) gameObjIds[i]);
                if (selectedObj.getComponent(IsNotSelectable.class) == null) {
                    return true;
                }
            }
        }

        return false;
    }
}

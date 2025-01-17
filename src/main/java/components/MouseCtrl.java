package components;

import TCB_Field.GameObject;
import TCB_Field.KeyListener;
import TCB_Field.MouseListener;
import TCB_Field.Window;
import org.joml.Vector4f;
import utility.Settings;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

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

    // A state register for object placement event. Only place an object when the mouse button is released.
    private boolean mouseButtonHeld = false;
    private float currentX, currentY, pastX, pastY;

    // Debug value, checking holdObj with the last placed Object.
    // Investigating the issue where cancel placement (pressing escape)
    // will cause last placed object to disappear from the current scene.
    private GameObject lastPlaced = null;


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
        // Return coordinate base position from raw mouse input to place an active object in standard position.
        if (holdObj != null) {
            holdObj.transform.position.x = MouseListener.getWorldX() - Settings.GRID_WIDTH / 2.0f; // Might not need to - 0.16f for both
            holdObj.transform.position.y = MouseListener.getWorldY() - Settings.GRID_HEIGHT / 2.0f;
            holdObj.transform.position.x = Math.round(holdObj.transform.position.x / Settings.GRID_WIDTH) * Settings.GRID_WIDTH + Settings.GRID_WIDTH / 2.0f;
            holdObj.transform.position.y = Math.round(holdObj.transform.position.y / Settings.GRID_HEIGHT) * Settings.GRID_HEIGHT + Settings.GRID_HEIGHT / 2.0f;
            currentX = holdObj.transform.position.x;
            currentY = holdObj.transform.position.y;

            // When click released, place the object.
            if (!MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT) && mouseButtonHeld) {
                placeObj();
                mouseButtonHeld = false;
            }

            // Register click event
            if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
                mouseButtonHeld = true;

            }

            // Remove the current selected object to be place from the scene.
            if (KeyListener.isKeyPressed(GLFW_KEY_ESCAPE)) {
                //System.out.println("Holding obj: " + holdObj.loadUid());
                //System.out.println("Last placed object: " + lastPlaced.loadUid());
                this.holdObj.destroy();
                this.holdObj = null;
            }
        }
    }

}

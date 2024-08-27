package components;

import TCB_Field.GameObject;
import TCB_Field.MouseListener;
import TCB_Field.Window;
import org.joml.Vector2f;
import utility.Settings;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class MouseCtrl extends Component {
    GameObject holdObj = null;

    public void pickObj(GameObject obj) {
        this.holdObj = obj;
        Window.getScene().addObjToScene(obj);
    }

    public void placeObj() {
        this.holdObj = null;
    }

    @Override
    public void editorUpdate(float dt) {
        // Return coordinate base position from raw mouse input to place active object in standard position.
        if (holdObj != null) {
            holdObj.transform.position.x = MouseListener.getOrthoX(); // Might not need to - 0.16f for both
            holdObj.transform.position.y = MouseListener.getOrthoY();
            holdObj.transform.position.x = Math.round(holdObj.transform.position.x / Settings.GRID_WIDTH) * Settings.GRID_WIDTH;
            holdObj.transform.position.y = Math.round(holdObj.transform.position.y / Settings.GRID_HEIGHT) * Settings.GRID_HEIGHT;
            if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
                placeObj();
            }
        }
    }
}

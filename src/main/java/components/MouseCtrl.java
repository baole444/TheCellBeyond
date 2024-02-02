package components;

import TCB_Field.GameObject;
import TCB_Field.MouseListener;
import TCB_Field.Window;
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
    public void updateEditor(float dt) {
        if (holdObj != null) {
            holdObj.transform.position.x = MouseListener.getOrthoX() - 0.16f;
            holdObj.transform.position.y = MouseListener.getOrthoY() - 0.16f;
            holdObj.transform.position.x = (int)(holdObj.transform.position.x / Settings.GRID_WIDTH) * Settings.GRID_WIDTH;
            holdObj.transform.position.y = (int)(holdObj.transform.position.y / Settings.GRID_HEIGHT) * Settings.GRID_HEIGHT;
            if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
                placeObj();
            }
        }
    }
}

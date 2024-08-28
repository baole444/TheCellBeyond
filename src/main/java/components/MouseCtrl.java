package components;

import TCB_Field.GameObject;
import TCB_Field.KeyListener;
import TCB_Field.MouseListener;
import TCB_Field.Window;
import org.joml.Vector4f;
import utility.Settings;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class MouseCtrl extends Component {
    GameObject holdObj = null;
    private float clickReleaseTime = 0.35f; // Use to signal the debounce time
    private float clickRelease = clickReleaseTime; // Reset value


    public void pickObj(GameObject obj) {
        if (this.holdObj != null) {
            // Use to prevent placing old object at edge of viewport
            // When selecting new sprite to place.
            this.holdObj.destroy();
        }
        this.holdObj = obj;
        this.holdObj.getComponent(SpriteRender.class).setColor(new Vector4f(1f, 1f, 1f, 0.5f));
        this.holdObj.addComponent(new IsNotSelectable());
        Window.getScene().addObjToScene(obj);
    }

    public void placeObj() {
        GameObject newObj = this.holdObj.copy();
        this.holdObj.transform.zIndex = 0;
        this.holdObj.destroy();
        newObj.getComponent(SpriteRender.class).setColor(new Vector4f(1, 1, 1, 1));
        newObj.removeComponent(IsNotSelectable.class);
        Window.getScene().addObjToScene(newObj);
    }

    @Override
    public void editorUpdate(float dt) {
        clickRelease -= dt;
        // Return coordinate base position from raw mouse input to place active object in standard position.
        if (holdObj != null && clickRelease <= 0) {
            holdObj.transform.position.x = MouseListener.getWorldX() - 0.16f; // Might not need to - 0.16f for both
            holdObj.transform.position.y = MouseListener.getWorldY() - 0.16f;
            holdObj.transform.position.x = Math.round(holdObj.transform.position.x / Settings.GRID_WIDTH) * Settings.GRID_WIDTH + Settings.GRID_WIDTH / 2.0f;
            holdObj.transform.position.y = Math.round(holdObj.transform.position.y / Settings.GRID_HEIGHT) * Settings.GRID_HEIGHT + Settings.GRID_HEIGHT / 2.0f;
            if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
                placeObj();
                clickRelease = clickReleaseTime;
            }

            if (KeyListener.isKeyTapped(GLFW_KEY_ESCAPE)) {
                holdObj.destroy();
                holdObj = null;
            }
        }
    }

}

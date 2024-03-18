package components;

import TCB_Field.GameObject;
import TCB_Field.KeyListener;
import TCB_Field.MouseListener;
import TCB_Field.Window;
import org.joml.Vector4f;
import render.ObjectSelection;
import scene.Scene;
import utility.Settings;

import static org.lwjgl.glfw.GLFW.*;
//TODO: Fix this entire class, it is quite broken.
public class MouseCtrl extends Component {
    GameObject holdObj = null;
    private float holdInit = 0.03f;
    private float unHold = holdInit;

    public void pickObj(GameObject obj) {
        // remove ghost obj
        if (this.holdObj != null) {
            this.holdObj.destroy();
        }
        this.holdObj = obj;

        this.holdObj.getComponent(SpriteRender.class).setColor(new Vector4f(0.8f, 0.8f, 0.8f, 0.5f));
        this.holdObj.addComponent(new IsNotSelectable());

        Window.getScene().addObjToScene(obj);
    }
    //TODO: update this
    public void placeObj() {
        GameObject dupObj = this.holdObj.duplicate();
        dupObj.getComponent(SpriteRender.class).setColor(new Vector4f(1, 1, 1, 1));
        dupObj.removeComponent(IsNotSelectable.class);
        Window.getScene().addObjToScene(dupObj);

        this.holdObj = null;
    }

    @Override
    public void updateEditor(float dt) {
        unHold -= dt;
        ObjectSelection objectSelection = Window.loadImGui().loadProperties().loadObjSelection();
        Scene currentScene = Window.getScene();
        if (holdObj != null && unHold <= 0.0f) {
            float x = MouseListener.getWorldX();
            float y = MouseListener.getWorldY();
            holdObj.transform.position.x = ((int)Math.floor(x / Settings.GRID_WIDTH) * Settings.GRID_WIDTH) + Settings.GRID_WIDTH / 2.0f;
            holdObj.transform.position.y = ((int)Math.floor(y / Settings.GRID_HEIGHT) * Settings.GRID_HEIGHT) + Settings.GRID_HEIGHT / 2.0f;
            if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
                placeObj();
                unHold = holdInit;
            }

            if (KeyListener.isKeyTapped(GLFW_KEY_ESCAPE)) {
                holdObj.destroy();
                holdObj = null;
            }
        } else if (!MouseListener.isDragging() && MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT) && unHold < 0) {
            int x = (int)MouseListener.loadScrX();
            int y = (int)MouseListener.loadScrY();
            int gObjectId = objectSelection.pixelCheck(x, y);
            GameObject selectedObj = currentScene.loadGameObj(gObjectId);
            // Excluding the gizmo
            if (selectedObj != null && selectedObj.getComponent(IsNotSelectable.class) == null) {
                Window.loadImGui().loadProperties().setActiveObj(selectedObj);
                System.out.println("Clicked on" + selectedObj + "registered. Did you saw the gizmo?");
            } else if (selectedObj == null && !MouseListener.isDragging()) {
                Window.loadImGui().loadProperties().clearSelectedObj();
            }
            this.unHold = 0.2f;
        }
    }
}

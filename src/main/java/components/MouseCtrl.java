package components;

import TCB_Field.GameObject;
import TCB_Field.KeyListener;
import TCB_Field.MouseListener;
import TCB_Field.Window;
import editor.Properties;
import org.joml.Vector2f;
import org.joml.Vector2i;
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
        if (dupObj.getComponent(StateEngine.class) != null) {
            //dupObj.getComponent(StateEngine.class).refreshTextures();
        }
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
        if (holdObj != null) {
            float x = MouseListener.getWorldX();
            float y = MouseListener.getWorldY();
            holdObj.transform.position.x = ((int)Math.floor(x / Settings.GRID_WIDTH) * Settings.GRID_WIDTH) + Settings.GRID_WIDTH / 2.0f;
            holdObj.transform.position.y = ((int)Math.floor(y / Settings.GRID_HEIGHT) * Settings.GRID_HEIGHT) + Settings.GRID_HEIGHT / 2.0f;

            if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
                float halfWd = Settings.GRID_WIDTH / 2.0f;
                float halfHt = Settings.GRID_HEIGHT / 2.0f;
                if (MouseListener.isDragging()
                        && !blockInSqr(holdObj.transform.position.x - halfWd,
                        holdObj.transform.position.y - halfHt)
                    )
                {
                    placeObj();
                } else if (!MouseListener.isDragging() && unHold < 0) {
                    placeObj();
                    unHold = holdInit;
                }
            }

            if (KeyListener.isKeyTapped(GLFW_KEY_ESCAPE)) {
                holdObj.destroy();
                holdObj = null;
            }
        } else if (!MouseListener.isDragging()
                && MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT)
                && unHold < 0)
        {
            int x = (int)MouseListener.loadScrX();
            int y = (int)MouseListener.loadScrY();
            int gObjectId = objectSelection.pixelCheck(x, y);

            System.out.println(gObjectId);

            GameObject selectedObj = currentScene.loadGameObj(gObjectId);
            // Excluding the gizmo
            if (selectedObj != null && selectedObj.getComponent(IsNotSelectable.class) == null) {
                Window.loadImGui().loadProperties().setActiveObj(selectedObj);
                System.out.println("Clicked on '" + selectedObj + "' registered. Did you see the gizmo?");
            } else if (selectedObj == null && !MouseListener.isDragging()) {
                Window.loadImGui().loadProperties().clearSelectedObj();
            }
            this.unHold = 0.2f;
        }
    }


    private boolean blockInSqr(float x, float y) {
        Properties properties = Window.loadImGui().loadProperties();

        Vector2f begin = new Vector2f(x, y);
        Vector2f end = new Vector2f(begin).add(new Vector2f(Settings.GRID_WIDTH, Settings.GRID_HEIGHT));

        Vector2f beginScrF = MouseListener.world2Scr(begin);
        Vector2f endScrF = MouseListener.world2Scr(end);

        Vector2i beginScr = new Vector2i((int)beginScrF.x + 2, (int)beginScrF.y + 2);
        Vector2i endScr = new Vector2i((int)endScrF.x - 2, (int)endScrF.y -2);

        float[] gameObjID = properties.loadObjSelection().pixelChecks(beginScr, endScr);

        for (int i = 0; i < gameObjID.length; i++) {
            if (gameObjID[i] >= 0) {
                GameObject selectedObj = Window.getScene().loadGameObj((int)gameObjID[i]);
                if (selectedObj.getComponent(IsNotSelectable.class) == null) {
                    return true;
                }
            }
        }

        return false;
    }
}

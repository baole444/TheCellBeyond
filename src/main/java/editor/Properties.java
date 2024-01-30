package editor;

import TCB_Field.GameObject;
import TCB_Field.MouseListener;
import imgui.ImGui;
import render.ObjectSelection;
import scene.Scene;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class Properties {
    private GameObject activeGameObject = null;
    private ObjectSelection objectSelection;

    private float clickInit = 0.2f;

    public Properties(ObjectSelection objectSelection) {
        this.objectSelection = objectSelection;
    }

    public void update(float dt, Scene currentScene) {
        clickInit -= dt;
        if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT) && clickInit < 0) {
            int x = (int)MouseListener.loadScrX();
            int y = (int)MouseListener.loadScrY();
            int gObjectId = objectSelection.pixelCheck(x, y);
            activeGameObject = currentScene.loadGameObj(gObjectId);
            this.clickInit = 0.2f;
        }
    }

    public void imgui() {
        if (activeGameObject != null) {
            ImGui.begin("Object properties");
            activeGameObject.imgui();
            ImGui.end();
        }
    }

    public GameObject loadActiveObj() {
        return this.activeGameObject;
    }
}

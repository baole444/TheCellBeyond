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

    public Properties(ObjectSelection objectSelection) {
        this.objectSelection = objectSelection;
    }

    public void update(float dt, Scene currentScene) {
        if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT)) {
            int x = (int)MouseListener.loadScrX();
            int y = (int)MouseListener.loadScrY();
            int gObjectId = objectSelection.pixelCheck(x, y);
            activeGameObject = currentScene.loadGameObj(gObjectId);
        }
    }

    public void imgui() {
        if (activeGameObject != null) {
            ImGui.begin("Object properties");
            activeGameObject.imgui();
            ImGui.end();
        }
    }
}

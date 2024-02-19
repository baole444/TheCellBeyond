package editor;

import TCB_Field.GameObject;
import TCB_Field.MouseListener;
import components.IsNotSelectable;
import imgui.ImGui;
import physic_2d.components.HardObject;
import physic_2d.components.collider.Collider2D;
import physic_2d.components.collider.ColliderCircle;
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
        if (!MouseListener.isDragging() && MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT) && clickInit < 0) {
            int x = (int)MouseListener.loadScrX();
            int y = (int)MouseListener.loadScrY();
            int gObjectId = objectSelection.pixelCheck(x, y);
            GameObject selectedObj = currentScene.loadGameObj(gObjectId);
            // Excluding the gizmo
            if (selectedObj != null && selectedObj.getComponent(IsNotSelectable.class) == null) {
                activeGameObject = selectedObj;
            } else if (selectedObj == null && !MouseListener.isDragging()) {
                activeGameObject = null;
            }
            this.clickInit = 0.2f;
        }
    }

    public void imgui() {
        if (activeGameObject != null) {
            ImGui.begin("Object properties");

            if (ImGui.beginPopupContextWindow("AddComponent")) {
                if (ImGui.menuItem("Generate Hard Object")) {
                    // TODO: add support for multi component item
                    if (activeGameObject.getComponent(HardObject.class) == null) {
                        activeGameObject.addComponent(new HardObject());
                    }
                }

                if (ImGui.menuItem("Generate Box Collider")) {
                    if ((activeGameObject.getComponent(Collider2D.class) == null) &&
                            (activeGameObject.getComponent(ColliderCircle.class) == null)) {
                        activeGameObject.addComponent(new Collider2D());
                    }
                }

                if (ImGui.menuItem("Generate Circle Collider")) {
                    if ((activeGameObject.getComponent(ColliderCircle.class) == null) &&
                            (activeGameObject.getComponent(Collider2D.class) == null)) {
                        activeGameObject.addComponent(new ColliderCircle());
                    }
                }
                ImGui.endPopup();
            }


            activeGameObject.imgui();
            ImGui.end();
        }
    }

    public GameObject loadActiveObj() {
        return this.activeGameObject;
    }

    public void setActiveObj(GameObject obj) {
        this.activeGameObject = obj;
    }
}

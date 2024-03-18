package editor;

import TCB_Field.GameObject;
import components.SpriteRender;
import imgui.ImGui;
import org.joml.Vector4f;
import physic_2d.components.HardObject;
import physic_2d.components.collider.Collider2D;
import physic_2d.components.collider.ColliderCircle;
import render.ObjectSelection;

import java.util.ArrayList;
import java.util.List;

public class Properties {
    private GameObject activeGameObject = null;
    private ObjectSelection objectSelection;
    private List<Vector4f> activeObjColor;
    private List<GameObject> activeGameObjects;
    private float clickInit = 0.2f;

    public Properties(ObjectSelection objectSelection) {
        this.objectSelection = objectSelection;
        this.activeObjColor = new ArrayList<>();
        this.activeGameObjects = new ArrayList<>();
    }

    /*
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
                System.out.println("Clicked on" + selectedObj + "registered. Did you see the gizmo?");
            } else if (selectedObj == null && !MouseListener.isDragging()) {
                activeGameObject = null;
            }
            this.clickInit = 0.2f;
        }
    }
    // Migrated to MouseCtrl
    */

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
        return activeGameObjects.size() == 1 ? this.activeGameObjects.get(0) : null;
    }

    public List<GameObject> getActiveGameObjects() {
        return this.activeGameObjects;
    }

    public void setActiveObj(GameObject obj) {
        if (obj != null) {
            clearSelectedObj();
            this.activeGameObjects.add(obj);
        }
    }

    public void addActiveObj(GameObject obj) {
        SpriteRender spr = obj.getComponent(SpriteRender.class);
        if (spr != null) {
            this.activeObjColor.add(new Vector4f(spr.loadColor()));
            spr.setColor(new Vector4f(0.8f, 0.8f, 0.0f, 0.8f));
        } else {
            this.activeObjColor.add(new Vector4f());
        }
        this.activeGameObjects.add(obj);
    }

    public void clearSelectedObj() {
        if (activeObjColor.size() > 0) {
            int i = 0;
            for (GameObject go : activeGameObjects) {
                SpriteRender spr = go.getComponent(SpriteRender.class);
                if (spr != null) {
                    spr.setColor(activeObjColor.get(i));
                }
                i++;
            }
        }
        this.activeGameObjects.clear();
        this.activeObjColor.clear();
    }

    public ObjectSelection loadObjSelection() {
        return this.objectSelection;
    }
}

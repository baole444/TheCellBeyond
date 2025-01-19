package editor;

import TCB_Field.GameObject;
import TCB_Field.MouseListener;
import components.IsNotSelectable;
import components.SpriteRender;
import imgui.ImGui;
import org.joml.Vector4f;
import physic_2d.components.HardObject;
import physic_2d.components.collider.Collider2D;
import physic_2d.components.collider.ColliderCircle;
import render.ObjectSelection;
import scene.Scene;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class Properties {
    private GameObject activeGameObject = null;
    private List<GameObject> activeGameObjects;
    private List<Vector4f> activeObjTrueColor;
    private ObjectSelection objectSelection;


    public Properties(ObjectSelection objectSelection) {
        this.activeGameObjects = new ArrayList<>();
        this.objectSelection = objectSelection;
        this.activeObjTrueColor = new ArrayList<>();
    }

    public void imgui() {
        if (activeGameObjects.size() == 1 && activeGameObjects.getFirst() != null) {
            activeGameObject = activeGameObjects.getFirst();

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

    public void addActiveObj(GameObject go) {
        SpriteRender spriteRender = go.getComponent(SpriteRender.class);
        if (spriteRender != null) {
            this.activeObjTrueColor.add(new Vector4f(spriteRender.loadColor()));
            // I like this color, but more testing with user feedbacks will be more valuable.
            // This is orange
            //spriteRender.setColor(new Vector4f(1f, 0.8f, 0.6f, 0.5f));

            // This is yellow
            spriteRender.setColor(new Vector4f(1f, 1f, 0.6f, 0.5f));

            // This is blue
            //spriteRender.setColor(new Vector4f(0.6f, 1f, 1f, 0.5f));
        } else {
            this.activeObjTrueColor.add(new Vector4f());
        }

        this.activeGameObjects.add(go);

    }

    public GameObject loadActiveObj() {
        if (activeGameObjects.size() == 1) {
            return this.activeGameObjects.getFirst();
        } else {
            return null;
        }
    }

    public List<GameObject> loadAllActiveObj() {
        return this.activeGameObjects;
    }

    public void setActiveGameObj(GameObject go) {
        if (go != null) {
            clearSelection();
            this.activeGameObjects.add(go);
        }
    }

    public ObjectSelection loadObjSelection() {
        return this.objectSelection;
    }

    public void clearSelection() {
        if (activeObjTrueColor.size() > 0) {
            int i = 0;
            for (GameObject go : activeGameObjects) {
                SpriteRender spriteRender = go.getComponent(SpriteRender.class);
                if (spriteRender != null) {
                    spriteRender.setColor(activeObjTrueColor.get(i));
                }
                i++;
            }
        }
        this.activeGameObjects.clear();
        this.activeObjTrueColor.clear();
    }
}

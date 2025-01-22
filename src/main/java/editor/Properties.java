package editor;

import TCB_Field.GameObject;
import components.SpriteRender;
import imgui.ImGui;
import org.joml.Vector4f;
import physic_2d.components.FlatPhysicBody;
import physic_2d.components.collider.FlatBoxCollider;
import physic_2d.components.collider.FlatCircleCollider;
import render.ObjectSelection;

import java.util.ArrayList;
import java.util.List;

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
                if (ImGui.menuItem("Generate Physic body")) {
                    if (activeGameObject.getComponent(FlatPhysicBody.class) == null) {
                        activeGameObject.addComponent(new FlatPhysicBody());
                    }
                }

                if (ImGui.menuItem("Generate Box Collider")) {
                    if ((activeGameObject.getComponent(FlatBoxCollider.class) == null) &&
                            (activeGameObject.getComponent(FlatCircleCollider.class) == null)) {
                        activeGameObject.addComponent(new FlatBoxCollider());
                    }
                }

                if (ImGui.menuItem("Generate Circle Collider")) {
                    if ((activeGameObject.getComponent(FlatCircleCollider.class) == null) &&
                            (activeGameObject.getComponent(FlatBoxCollider.class) == null)) {
                        activeGameObject.addComponent(new FlatCircleCollider());
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

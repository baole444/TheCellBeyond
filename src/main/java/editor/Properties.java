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

    /**
     * Preserve sprite's original color for recovery after selection.
     */
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

    /**
     * Add an active game object to {@link #activeGameObjects}.
     * Also copy the object's sprite's color attribute to {@link #activeObjTrueColor}.
     * @param go The desired {@link GameObject} that wanted to be set active.
     */
    public void addActiveObj(GameObject go) {
        SpriteRender spriteRender = go.getComponent(SpriteRender.class);
        if (spriteRender != null) {
            //TODO: Allow user to select their preferred highlighting color.
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

    /**
     * Get the first active game object.
     * Used when there is currently only one active game object.
     * @return first element of {@link #activeGameObjects}.
     */
    public GameObject loadActiveObj() {
        if (activeGameObjects.size() == 1) {
            return this.activeGameObjects.getFirst();
        } else {
            return null;
        }
    }

    /**
     * Get all active game objects.
     * @return reference to {@link  #activeGameObjects}
     */
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

    /**
     * Create a shallow copy of {@link #activeObjTrueColor}.
     * @return a new ArrayList of active objects' true color.
     */
    public List<Vector4f> getActiveObjTrueColor() {
        return new ArrayList<>(activeObjTrueColor);
    }

    /**
     * Clear the active object list.
     * The user's object selection will be cleared.
     * Also clear true color list after resetting all sprites' original color.
     */
    public void clearSelection() {
        if (!activeObjTrueColor.isEmpty()) {
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

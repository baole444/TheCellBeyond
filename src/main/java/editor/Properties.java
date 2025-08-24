package editor;

import TheCellBeyond.GameObject;
import components.SpriteRenderer;
import editor.dialog.AddComponentDialog;
import imgui.ImGui;
import org.joml.Vector4f;
import physic2d.components.PhysicBody2D;
import physic2d.components.collider.BoxCollider2D;
import physic2d.components.collider.CircleCollider2D;
import render.ObjectSelection;

import java.util.ArrayList;
import java.util.List;

public class Properties {
    private static final String PROPERTY_SECTION_ID = "Object_Component_Section";
    private static final int BUTTON_RESERVED_HEIGHT = 36;
    private GameObject activeGameObject = null;
    private final List<GameObject> activeGameObjects;

    /**
     * Preserve sprite's original color for recovery after selection.
     */
    private final List<Vector4f> activeObjTrueColor;

    private final ObjectSelection objectSelection;

    public Properties(ObjectSelection objectSelection) {
        this.activeGameObjects = new ArrayList<>();
        this.objectSelection = objectSelection;
        this.activeObjTrueColor = new ArrayList<>();
    }

    public void imgui() {
        if (activeGameObjects.size() == 1 && activeGameObjects.getFirst() != null) {
            activeGameObject = activeGameObjects.getFirst();

            ImGui.begin("Object properties");

            float buttonW = ImGui.getContentRegionAvailX();
            float buttonH = BUTTON_RESERVED_HEIGHT * 0.9f;
            if (ImGui.button("Add new Component", buttonW, buttonH)) AddComponentDialog.show(activeGameObject);

            ImGui.separator();

            activeGameObject.imgui();

            renderContextMenu();

            AddComponentDialog.imgui();

            ImGui.end();
        }
    }

    private void renderContextMenu() {
        if (ImGui.beginPopupContextWindow("AddComponent")) {
            if (ImGui.menuItem("Generate Physic body")) {
                if (activeGameObject.getFirstComponent(PhysicBody2D.class) == null) {
                    activeGameObject.addComponent(new PhysicBody2D());
                }
            }

            if (ImGui.menuItem("Generate Box Collider")) {
                if ((activeGameObject.getFirstComponent(BoxCollider2D.class) == null) &&
                        (activeGameObject.getFirstComponent(CircleCollider2D.class) == null)) {
                    activeGameObject.addComponent(new BoxCollider2D());
                }
            }

            if (ImGui.menuItem("Generate Circle Collider")) {
                if ((activeGameObject.getFirstComponent(CircleCollider2D.class) == null) &&
                        (activeGameObject.getFirstComponent(BoxCollider2D.class) == null)) {
                    activeGameObject.addComponent(new CircleCollider2D());
                }
            }
            ImGui.endPopup();
        }
    }

    /**
     * Add an active game object to {@link #activeGameObjects}.
     * Also copy the object's sprite's color attribute to {@link #activeObjTrueColor}.
     * @param go The desired {@link GameObject} that wanted to be set active.
     */
    public void addActiveGameObject(GameObject go) {
        SpriteRenderer spriteRenderer = go.getFirstComponent(SpriteRenderer.class);
        if (spriteRenderer != null) {
            //TODO: Allow user to select their preferred highlighting color.
            this.activeObjTrueColor.add(new Vector4f(spriteRenderer.getColor()));
            // I like this color, but more testing with user feedbacks will be more valuable.
            // This is orange
            //spriteRenderer.setColor(new Vector4f(1f, 0.8f, 0.6f, 0.5f));

            // This is yellow
            spriteRenderer.setColor(new Vector4f(1f, 1f, 0.6f, 0.5f));

            // This is blue
            //spriteRenderer.setColor(new Vector4f(0.6f, 1f, 1f, 0.5f));
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
    public GameObject getActiveGameObject() {
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
    public List<GameObject> getActiveGameObjects() {
        return this.activeGameObjects;
    }

    public void setActiveGameObject(GameObject go) {
        if (go != null) {
            clearSelection();
            this.activeGameObjects.add(go);
        }
    }

    public ObjectSelection getObjectSelection() {
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
                SpriteRenderer spriteRenderer = go.getFirstComponent(SpriteRenderer.class);
                if (spriteRenderer != null) {
                    spriteRenderer.setColor(activeObjTrueColor.get(i));
                }
                i++;
            }
        }
        this.activeGameObjects.clear();
        this.activeObjTrueColor.clear();
    }
}

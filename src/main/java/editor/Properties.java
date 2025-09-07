package editor;

import TheCellBeyond.GameObject;
import components.SpriteRenderer;
import editor.dialog.AddComponentDialog;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import org.joml.Vector4f;
import physic2d.components.PhysicBody2D;
import physic2d.components.collider.BoxCollider2D;
import physic2d.components.collider.CircleCollider2D;
import render.ObjectSelection;
import render.texture.Sprite;

import java.util.ArrayList;
import java.util.List;

public class Properties {
    public static final String WINDOW_ID = "Inspector###Object_Properties";
    private static final int BUTTON_RESERVED_HEIGHT = 36;
    private GameObject activeGameObject = null;
    private final List<GameObject> activeGameObjects;

    /**
     * Preserve sprite's original color for recovery after selection.
     */
    private final List<List<Vector4f>> activeObjTrueColor;

    private final ObjectSelection objectSelection;

    public Properties(ObjectSelection objectSelection) {
        activeGameObjects = new ArrayList<>();
        this.objectSelection = objectSelection;
        activeObjTrueColor = new ArrayList<>();
    }

    public void imgui() {
        ImGui.begin(WINDOW_ID, ImGuiWindowFlags.NoCollapse);

        if (activeGameObjects.size() == 1 && activeGameObjects.getFirst() != null) {
            activeGameObject = activeGameObjects.getFirst();
        }

        if (activeGameObject == null) {
            ImGui.beginDisabled();
            ImGui.textWrapped("Select an object in the Scene tree or in the scene to edit its properties");
            ImGui.endDisabled();
            ImGui.end();
            return;
        }

        float buttonW = ImGui.getContentRegionAvailX();
        float buttonH = BUTTON_RESERVED_HEIGHT * 0.9f;
        if (ImGui.button("Add new Component", buttonW, buttonH)) AddComponentDialog.show(activeGameObject);

        ImGui.separator();
        activeGameObject.imgui();
        renderContextMenu();
        AddComponentDialog.imgui();
        ImGui.end();
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
        List<Vector4f> colors = new ArrayList<>();
        for (SpriteRenderer sprite : go.getComponents(SpriteRenderer.class)) {
            if (sprite != null) colors.add(sprite.getColor());
        }
        activeObjTrueColor.add(colors);
        activeGameObjects.add(go);
    }

    /**
     * Get the first active game object.
     * Used when there is currently only one active game object.
     * @return first element of {@link #activeGameObjects}.
     */
    public GameObject getActiveGameObject() {
        if (activeGameObjects.size() == 1) {
            return activeGameObjects.getFirst();
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
            activeGameObjects.add(go);
        }
    }

    public ObjectSelection getObjectSelection() {
        return objectSelection;
    }

    public List<List<Vector4f>> getActiveObjTrueColor() {
        return new ArrayList<>(activeObjTrueColor);
    }

    /**
     * Clear the active object list.
     * The user's object selection will be cleared.
     * Also clear true color list after resetting all sprites' original color.
     */
    public void clearSelection() {
        if (!activeObjTrueColor.isEmpty()) {
            for (int i = 0; i < activeGameObjects.size(); i++) {
                GameObject go = activeGameObjects.get(i);
                List<Vector4f> colors = activeObjTrueColor.get(i);
                List<SpriteRenderer> sprites = go.getComponents(SpriteRenderer.class);
                for (int j = 0; j < sprites.size(); j++) {
                    SpriteRenderer sprite = sprites.get(j);
                    if (sprite != null) sprite.setColor(colors.get(j));
                }
            }
        }
        activeGameObjects.clear();
        activeObjTrueColor.clear();
    }
}

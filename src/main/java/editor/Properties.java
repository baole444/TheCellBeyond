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
    private static GameObject activeGameObject = null;
    private static final List<GameObject> activeGameObjects = new ArrayList<>();
    private static final List<List<Vector4f>> activeObjTrueColor = new ArrayList<>();

    public static void imgui() {
        if (!ImGui.begin(WINDOW_ID, ImGuiWindowFlags.NoCollapse)) {
            ImGui.end();
            return;
        }

        activeGameObject = getActiveGameObject();

        if (activeGameObject == null) {
            ImGui.beginDisabled();
            ImGui.textWrapped("Select an object in the Scene tree or in the scene to edit its properties");
            ImGui.endDisabled();
            ImGui.end();
            return;
        }

        if (activeGameObject.isRemoved()) {
            activeGameObject = null;
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

        updateActives();
    }

    private static void renderContextMenu() {
        if (ImGui.beginPopupContextWindow("AddComponent")) {
            ImGui.textWrapped("This menu will be rework soon");
            ImGui.endPopup();
        }
    }

    /**
     * Add an active game object to {@link #activeGameObjects}.
     * Also copy the object's sprite's color attribute to {@link #activeObjTrueColor}.
     * @param go The desired {@link GameObject} that wanted to be set active.
     */
    public static void addActiveGameObject(GameObject go) {
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
    public static GameObject getActiveGameObject() {
        if (activeGameObjects.isEmpty()) return null;

        return activeGameObjects.getFirst();
    }

    /**
     * Get all active game objects.
     * @return reference to {@link #activeGameObjects}
     */
    public static List<GameObject> getActiveGameObjects() {
        return activeGameObjects;
    }

    public static void setActiveGameObject(GameObject go) {
        if (go != null) {
            if (go != getActiveGameObject()) BottomPanel.clear();
            clearSelection();
            activeGameObjects.add(go);
        }
    }

    public static List<List<Vector4f>> getActiveObjTrueColor() {
        return new ArrayList<>(activeObjTrueColor);
    }

    /**
     * Clear the active object list.
     * The user's object selection will be cleared.
     * Also clear true color list after resetting all sprites' original color.
     */
    public static void clearSelection() {
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

    private static void updateActives() {
        List<GameObject> actives = new ArrayList<>(activeGameObjects);
        for (GameObject go : actives) {
            if (go.isRemoved()) activeGameObjects.remove(go);
        }

    }
}


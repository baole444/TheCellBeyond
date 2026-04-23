package editor;

import TheCellBeyond.GameObject;
import components.SpriteRenderer;
import editor.dialog.AddComponentDialog;
import editor.template.EditorTemplate;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Properties hold the selected object inspector panel.
 */
public class Properties {
    public static final String WindowID = "Inspector###Object_Properties";
    private static final int ButtonReservedHeight = 36;
    private static final List<GameObject> activeGameObjects = new ArrayList<>();
    private static final List<List<Vector4f>> activeObjTrueColor = new ArrayList<>();

    /**
     * Render the inspector panel
     */
    public static void imgui() {
        if (!ImGui.begin(WindowID, ImGuiWindowFlags.NoCollapse)) {
            ImGui.end();
            return;
        }
        GameObject activeGameObject = getActiveGameObject();
        BottomPanel.interacted(activeGameObject);
        if (activeGameObject == null) {
            ImGui.beginDisabled();
            ImGui.textWrapped("Select an object in the Scene tree or in the scene to edit its properties");
            ImGui.endDisabled();
            ImGui.end();
            return;
        }
        if (activeGameObject.isDestroyed()) {
            updateActives();
            ImGui.end();
            return;
        }
        float buttonW = ImGui.getContentRegionAvailX();
        float buttonH = ButtonReservedHeight * 0.9f;
        if (ImGui.button("Add new Component##Properties_Inspector_Add_Component_Button", buttonW, buttonH)) AddComponentDialog.show(activeGameObject);
        ImGui.separator();
        EditorTemplate.render(activeGameObject);
        renderContextMenu();
        AddComponentDialog.imgui();
        ImGui.end();
        updateActives();
    }

    private static void renderContextMenu() {
        if (ImGui.beginPopupContextWindow("AddComponent")) {
            ImGui.text("This menu will be rework soon");
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
            if (sprite != null) colors.add(sprite.color());
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
        if (go == null) return;
        if (go != getActiveGameObject()) BottomPanel.clear();
        clearSelection();
        addActiveGameObject(go);
    }

    public static List<List<Vector4f>> getActiveObjTrueColor() {
        return new ArrayList<>(activeObjTrueColor);
    }

    /**
     * Clear the active object list.
     * The user's object selection will be cleared.
     * Also clear true colour list after resetting all sprites' original color.
     */
    public static void clearSelection() {
        if (!activeObjTrueColor.isEmpty()) {
            for (int i = 0; i < activeGameObjects.size(); i++) {
                GameObject go = activeGameObjects.get(i);
                List<Vector4f> colors = activeObjTrueColor.get(i);
                List<SpriteRenderer> sprites = go.getComponents(SpriteRenderer.class);
                if (sprites.isEmpty()) continue;
                for (int j = 0; j < sprites.size(); j++) {
                    SpriteRenderer sprite = sprites.get(j);
                    if (sprite != null && j < colors.size()) sprite.color(colors.get(j));
                }
            }
        }
        activeGameObjects.clear();
        activeObjTrueColor.clear();
    }

    private static void updateActives() {
        for (int i = activeGameObjects.size() - 1; i >= 0; i--) {
            if (!activeGameObjects.get(i).isDestroyed()) continue;
            activeGameObjects.remove(i);
            activeObjTrueColor.remove(i);
        }
    }
}


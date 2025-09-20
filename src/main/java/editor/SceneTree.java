package editor;

import TheCellBeyond.GameObject;
import TheCellBeyond.Window;
import editor.dialog.AddObjectDialog;
import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import scene.Scene;
import utility.prefabrication.PrefabManager;

import java.util.List;

public class SceneTree {
    public static final String WINDOW_ID = "Scene Tree###Editor_Current_Scene_Tree";
    private static final String GROUPING_PAYLOAD = "Object_Grouping_Payload";
    private static final String SCENE_TREE_ID = "Scene_Tree_Section";
    private static final String NEW_POPUP_ID = "New_Add_Object_Popup";
    private static final int BUTTON_RESERVED_HEIGHT = 36;
    private static final boolean enableBorder = true;
    private static GameObject selectedObject = null;

    public static void imgui() {
        ImGui.begin(WINDOW_ID, ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoCollapse);

        Scene scene = Window.getScene();

        if (scene == null) {
            ImGui.text("No scene loaded");
            ImGui.end();
            return;
        }

        List<GameObject> rootGameObjects = scene.getRootGameObjects();

        float buttonW = ImGui.getContentRegionAvailX();
        float buttonH = BUTTON_RESERVED_HEIGHT * 0.9f;
        if (ImGui.button("Add new Object", buttonW, buttonH)) AddObjectDialog.show(null);

        ImGui.separator();

        float availHeight = ImGui.getContentRegionAvailY() - BUTTON_RESERVED_HEIGHT * 1.1f;
        ImGui.beginChild(SCENE_TREE_ID, ImGuiWindowFlags.None, availHeight, enableBorder);
        if (rootGameObjects.isEmpty()) {
            ImGui.text("Scene is empty");
        } else {
            for (GameObject go : rootGameObjects) {
                if (go.isSerialize()) renderTree(go, scene);
            }
        }

        if (ImGui.isWindowHovered()
                && !ImGui.isAnyItemHovered()
                && ImGui.isMouseClicked(ImGuiMouseButton.Right)
        ) ImGui.openPopup(NEW_POPUP_ID);

        if (ImGui.beginPopup(NEW_POPUP_ID)) {
            if (ImGui.menuItem("New Object...")) AddObjectDialog.show(null);
            ImGui.endPopup();
        }
        ImGui.endChild();

        AddObjectDialog.imgui();

        ImGui.end();
    }

    public static void clearSelection() {
        selectedObject = null;
    }

    private static void renderTree(GameObject go, Scene scene) {
        ImGui.pushID(go.getUUID());

        int flags = ImGuiTreeNodeFlags.OpenOnArrow
                | ImGuiTreeNodeFlags.SpanAvailWidth
                | ImGuiTreeNodeFlags.FramePadding;

        if (go == selectedObject) {
            flags |= ImGuiTreeNodeFlags.Selected;
        }

        List<GameObject> children = go.getChildren().stream().toList();

        if (children.isEmpty()) {
            flags |= ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen;
        }

        boolean nodeOpen = ImGui.treeNodeEx(go.name, flags);
        renderContextMenu(go, scene);

        if (ImGui.isItemClicked() && !ImGui.isItemToggledOpen()) {
            selectedObject = go;
            Properties.setActiveGameObject(go);
        }

        if (ImGui.beginDragDropSource()) {
            ImGui.setDragDropPayload(GROUPING_PAYLOAD, go);
            ImGui.endDragDropSource();
        }

        if (ImGui.beginDragDropTarget()) {
            Object payload = ImGui.acceptDragDropPayload(GROUPING_PAYLOAD);

            if (payload instanceof GameObject dropGO) {
                if (scene.reparentObject(dropGO, go)) {
                    System.out.println("Reparented '" + dropGO.name + "' to '" + go.name + "'");
                } else {
                    System.out.println("Cannot reparented '" + dropGO.name + "' to '" + go.name + "'");
                }
            }

            ImGui.endDragDropTarget();
        }

        if (go.getParent() == null && ImGui.beginDragDropTarget()) {
            Object payload = ImGui.acceptDragDropPayload(GROUPING_PAYLOAD);

            if (payload instanceof GameObject dropGO) {
                if (scene.reparentObject(dropGO, null)) {
                    System.out.println("Reparented '" + dropGO.name + "' to root");
                }
            }

            ImGui.endDragDropTarget();
        }

        if (nodeOpen && !children.isEmpty()) {
            for (GameObject child : children) {
                if (child.isSerialize()) {
                    renderTree(child, scene);
                }
            }
            ImGui.treePop();
        }

        ImGui.popID();
    }

    private static void renderContextMenu(GameObject go, Scene scene) {
        if (ImGui.beginPopupContextItem()) {
            if (go == null || scene == null) return;

            if (ImGui.menuItem("Delete")) {
                go.destroy();
            }

            if (ImGui.beginMenu("Duplicate...")) {
                GameObject copy = null;

                if (ImGui.menuItem("Without children")) copy = go.copy();
                if (ImGui.menuItem("With children")) copy = go.copy(true);

                if (copy != null) {
                    copy.name = go.name + "_copy";
                    scene.queueForObjectAddition(copy, go.getParent());
                }

                ImGui.endMenu();
            }

            if (go.getParent() != null && ImGui.menuItem("Move to Root")) {
                scene.reparentObject(go, null);
            }

            ImGui.separator();

            if (ImGui.menuItem("Add child Object...")) {
                AddObjectDialog.show(go);
            }

            ImGui.separator();

            if (ImGui.beginMenu("Save as Prefab...")) {
                if (ImGui.menuItem("Without children")) savePrefabDialog(go, false);

                if (!go.getChildren().isEmpty() && ImGui.menuItem("With children")) savePrefabDialog(go, true);

                ImGui.endMenu();
            }

            ImGui.endPopup();
        }
    }

    private static void savePrefabDialog(GameObject go, boolean withChildren) {
        String prefabName = go.name.replaceAll("[^a-zA-z0-9_-]", "_");

        // TODO: add popup to ask custom prefab name later,
        //  auto fill it with current name in case user don't want to change it
        if (PrefabManager.get().savePrefab(go, prefabName, withChildren)) {
            System.out.println("Saved prefab: " + prefabName);
            return;
        }

        System.err.println("Failed to save prefab: " + prefabName);
    }
}

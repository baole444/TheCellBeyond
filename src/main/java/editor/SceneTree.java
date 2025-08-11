package editor;

import TheCellBeyond.GameObject;
import TheCellBeyond.Window;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import scene.Scene;
import utility.prefabrication.PrefabManager;

import java.util.List;

public class SceneTree {
    private static final String GROUPING_PAYLOAD = "ObjectGrouping";
    private GameObject selectedObject = null;

    public void imgui() {
        ImGui.begin("Scene Tree");

        Scene scene = Window.getScene();

        if (scene == null) {
            ImGui.text("");
            ImGui.end();
            return;
        }

        List<GameObject> rootGameObjects = scene.getRootGameObjects();

        if (rootGameObjects.isEmpty()) {
            ImGui.end();
            return;
        }

        for (GameObject go : rootGameObjects) {
            if (go.isSerialize()) {
                renderTree(go, scene);
            }
        }

        ImGui.end();
    }

    private void renderTree(GameObject go, Scene scene) {
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
            Window.getImGuiLayer().loadProperties().setActiveGameObject(go);
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

    private void renderContextMenu(GameObject go, Scene scene) {
        if (ImGui.beginPopupContextItem()) {
            if (go == null || scene == null) return;

            if (ImGui.menuItem("Delete")) {
                go.destroy();
            }

            if (ImGui.menuItem("Duplicate")) {
                GameObject copy = go.copy();
                copy.name = go.name + "_copy";
                scene.queueForObjectAddition(copy, go.getParent());
            }

            if (go.getParent() != null && ImGui.menuItem("Move to Root")) {
                scene.reparentObject(go, null);
            }

            ImGui.separator();

            if (ImGui.menuItem("Create Child")) {
                GameObject child = scene.generateObject("new_object");
                scene.queueForObjectAddition(child, go);
            }

            ImGui.separator();

            if (ImGui.beginMenu("Save as Prefab")) {
                if (ImGui.menuItem("Without children")) savePrefabDialog(go, false);

                if (!go.getChildren().isEmpty() && ImGui.menuItem("With children")) savePrefabDialog(go, true);

                ImGui.endMenu();
            }

            ImGui.endPopup();
        }
    }

    private void savePrefabDialog(GameObject go, boolean withChildren) {
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

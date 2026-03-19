package editor;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.internal.LogicServer;
import editor.dialog.AddObjectDialog;
import editor.dialog.ChooseRootTypeDialog;
import editor.payload.GameObjectDragDropPayload;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import physic2d.CharacterBody2D;
import scene.Scene;
import utility.log.EngineLog;
import utility.prefabrication.PrefabManager;

import java.util.List;

public class SceneTree {
    private static final EngineLog Logger = new EngineLog(SceneTree.class);
    public static final String WindowID = "Scene Tree###Editor_Current_Scene_Tree";
    private static final String SceneTreeID = "Scene_Tree_Section";
    private static final String NewPopupID = "New_Add_Object_Popup";
    private static final int ReservedButtonHeight = 36;
    private static final float ReorderingSpace = 4.0f;
    private static GameObject selectedObject = null;

    public static void imgui() {
        if (!ImGui.begin(WindowID, ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoCollapse)) {
            ImGui.end();
            return;
        }
        Scene scene = LogicServer.currentScene();
        if (scene == null) {
            renderQuickCreate();
            ImGui.end();
            return;
        }
        GameObject root = scene.root();
        if (root == null) {
            ImGui.text("Scene root object not found");
            ImGui.end();
            return;
        }
        float buttonW = ImGui.getContentRegionAvailX();
        float buttonH = ReservedButtonHeight * 0.9f;
        if (ImGui.button("Add new Object", buttonW, buttonH)) AddObjectDialog.show(root);
        ImGui.separator();
        ImGui.beginChild(SceneTreeID, new ImVec2(0.0f, 0.0f), ImGuiChildFlags.Border);
        renderHierarchyTree(root, scene);
        boolean openOrphans = ImGui.collapsingHeader("Orphan Objects##SceneTree_Orphan_Object_Header");
        if (openOrphans) renderOrphans(scene);
        if (ImGui.isWindowHovered() && !ImGui.isAnyItemHovered() && ImGui.isMouseClicked(ImGuiMouseButton.Right)) ImGui.openPopup(NewPopupID);
        if (ImGui.beginPopup(NewPopupID)) {
            if (ImGui.menuItem("New Object...")) AddObjectDialog.show(root);
            ImGui.endPopup();
        }
        ImGui.endChild();
        AddObjectDialog.imgui();
        ImGui.end();
    }

    private static void renderHierarchyTree(GameObject go, Scene scene) {
        if (go == null) return;
        renderTree(go, scene);
    }

    private static void renderOrphans(Scene scene) {
        if (scene == null) return;
        List<GameObject> orphans = scene.getOrphanObjects();
        orphans.forEach(go -> renderTree(go, scene));
    }

    private static void renderTree(GameObject go, Scene scene) {
        ImGui.pushID(go.getUUID().toString());
        int flags = ImGuiTreeNodeFlags.OpenOnArrow
                | ImGuiTreeNodeFlags.SpanAvailWidth
                | ImGuiTreeNodeFlags.FramePadding
                | ImGuiTreeNodeFlags.DefaultOpen;
        if (go == selectedObject) flags |= ImGuiTreeNodeFlags.Selected;
        List<GameObject> children = go.getChildren().stream().filter(GameObject::isSerialize).toList();
        if (children.isEmpty()) flags |= ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen;
        boolean nodeOpen = ImGui.treeNodeEx(go.name(), flags);
        renderContextMenu(go, scene);
        if (ImGui.isItemHovered() && ImGui.isMouseReleased(ImGuiMouseButton.Left) && !ImGui.isItemToggledOpen()) {
            selectedObject = go;
            Properties.setActiveGameObject(go);
        }
        if (ImGui.beginDragDropSource()) {
            GameObjectDragDropPayload.setPayload(go);
            ImGui.setDragDropPayload(GameObjectDragDropPayload.getPayloadType(), go);
            ImGui.text("Name: " + go.name());
            ImGui.text("Type: " + go.getClass().getSimpleName());
            ImGui.text("UUID: " + go.getUUID().toString());
            ImGui.endDragDropSource();
        }
        beginReparentDragDrop(go, scene);
        if (!nodeOpen || children.isEmpty()) {
            ImGui.popID();
            return;
        }
        children.forEach(child -> {
            renderInsertZone(child, scene, true);
            renderTree(child, scene);
        });
        renderInsertZone(children.getLast(), scene, false);
        ImGui.treePop();
        ImGui.popID();
    }

    private static void renderInsertZone(GameObject sibling, Scene scene, boolean insertBefore) {
        String order = insertBefore ? "before" : "after";
        String zoneID = "##" + "Reordering_Zone_" + order + "_" + sibling.getUUID();
        ImGui.invisibleButton(zoneID, ImGui.getContentRegionAvailX(), ReorderingSpace);
        if (!ImGui.beginDragDropTarget()) return;
        Object payload = ImGui.acceptDragDropPayload(GameObjectDragDropPayload.getPayloadType());
        if (!(payload instanceof GameObject dropGO)) {
            ImGui.endDragDropTarget();
            return;
        }
        if (scene.reorderObject(dropGO, sibling, insertBefore)) {
            Logger.info(String.format("Reordered '%s' %s '%s'", dropGO.name(), order, sibling.name()));
        } else {
            Logger.warning(String.format("Cannot reorder '%s' %s '%s'", dropGO.name(), order, sibling.name()));
        }
        ImGui.endDragDropTarget();
    }

    private static void beginReparentDragDrop(GameObject go, Scene scene) {
        if (!ImGui.beginDragDropTarget()) return;
        Object payload = ImGui.acceptDragDropPayload(GameObjectDragDropPayload.getPayloadType());
        if (!(payload instanceof GameObject dropGo)) {
            ImGui.endDragDropTarget();
            return;
        }
        if (scene.reparentObject(dropGo, go)) Logger.info(String.format("Reparented '%s' to '%s'", dropGo.name(), go.name()));
        else Logger.warning(String.format("Reparented '%s' to '%s' is not allowed!", dropGo.name(), go.name()));
        ImGui.endDragDropTarget();
    }

    private static void renderContextMenu(GameObject go, Scene scene) {
        if (go == null || scene == null) return;
        if (ImGui.beginPopupContextItem()) {
            if (ImGui.menuItem("Delete")) {
                scene.queueObjectForRemoval(go);
            }
            if (ImGui.beginMenu("Duplicate...")) {
                GameObject copy = null;
                if (ImGui.menuItem("Without children")) copy = go.copy();
                if (ImGui.menuItem("With children")) copy = go.copy(true);
                if (copy != null) {
                    copy.name(go.name() + "_copy");
                    scene.queueForObjectAddition(copy, go.getParent());
                }
                ImGui.endMenu();
            }
            if (go.getParent() != null && ImGui.menuItem("Move to Root")) scene.reparentObject(go, null);
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
        String prefabName = go.name().replaceAll("[^a-zA-z0-9_-]", "_");

        // TODO: add popup to ask custom prefab name later,
        //  auto fill it with current name in case user don't want to change it
        if (PrefabManager.get().savePrefab(go, prefabName, withChildren)) {
            System.out.println("Saved prefab: " + prefabName);
            return;
        }

        System.err.println("Failed to save prefab: " + prefabName);
    }

    public static void clearSelection() {
        selectedObject = null;
    }

    private static void renderQuickCreate() {
        ImGui.text("Create a new scene:");
        ImGui.separator();
        ImGui.spacing();
        float buttonW = ImGui.getContentRegionAvailX();
        if (ImGui.button("Create 2D Scene##ST_Quick_Create_2D_Scene", buttonW, 0.0f)) LogicServer.loadUnsavedScene(new GameObject2D());
        if (ImGui.isItemHovered()) ImGui.setTooltip("Create a new scene with root type as GameObject2D");
        ImGui.spacing();
        if (ImGui.button("Create 2D Character Scene##Quick_Create_2D_Character_Scene", buttonW, 0.0f)) LogicServer.loadUnsavedScene(new CharacterBody2D());
        if (ImGui.isItemHovered()) ImGui.setTooltip("Create a new scene with root type as CharacterBody2D");
        ImGui.spacing();
        if (ImGui.button("Other Scene type...", buttonW, 0.0f)) ChooseRootTypeDialog.show();
    }
}

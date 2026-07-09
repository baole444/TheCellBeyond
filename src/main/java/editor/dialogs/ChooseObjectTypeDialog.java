package editor.dialogs;

import TheCellBeyond.GameObject;
import TheCellBeyond.internal.LogicServer;
import editor.enums.ObjectType;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import scene.Scene;
import scene.SceneManager;
import scripting.ScriptLoader;
import scripting.TypeEntry;
import utility.log.EngineLog;

import java.util.List;

/**
 * Editor dialogue for selecting object type.
 */
public final class ChooseObjectTypeDialog {
    private static final String PopupID = "Choose Object Type";
    private static final String ObjectListID = "Object_Type_List";
    private static final String DescriptionSectionID = "Object_Description_Section";
    private static final ImVec2 DialogSize = new ImVec2(600.0f, 600.0f);
    private static final float ListYPercentage = 0.6f;
    private static final float DescriptionYPercentage = 0.2f;
    private static boolean showDialog = false;
    private static boolean replaceMode = false;
    private static ObjectType selectedType = null;
    private static TypeEntry selectedCustomType = null;
    private static GameObject targetObject = null;

    private ChooseObjectTypeDialog() {}

    /**
     * Toggle the show flag for this dialogue.
     */
    public static void show() {
        showDialog = true;
        replaceMode = false;
        selectedType = null;
        selectedCustomType = null;
    }

    /**
     * Toggle the show flag for this dialogue to replace mode for the current scene root.
     */
    public static void showReplaceRoot() {
        showReplace(null);
    }

    /**
     * Toggle the show flag for this dialogue to replace mode for the given object.
     * @param target the object to change type
     */
    public static void showReplace(GameObject target) {
        showDialog = true;
        replaceMode = true;
        selectedType = null;
        selectedCustomType = null;
        targetObject = target;
    }

    /**
     * Render the dialogue on screen.
     */
    public static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            String header = replaceMode ? "Select new object type:" : "Select root object type for the new scene:";
            ImGui.text(header);
            int sectionY = (int) (DialogSize.y * ListYPercentage);
            ImGui.beginChild(ObjectListID, 0.0f, sectionY, ImGuiChildFlags.Borders);
            for (ObjectType type : ObjectType.values()) {
                boolean isSelected = selectedType == type;
                if (ImGui.selectable(type.label + "##" + type.name(), isSelected)) {
                    selectedType = type;
                    selectedCustomType = null;
                }
            }
            List<TypeEntry> customTypes = ScriptLoader.gameObjectTypes();
            if (!customTypes.isEmpty()) {
                ImGui.separator();
                for (TypeEntry entry : customTypes) {
                    boolean isSelected = selectedCustomType == entry;
                    if (ImGui.selectable(entry.label() + "##Custom_" + entry.targetClass().getName(), isSelected)) {
                        selectedCustomType = entry;
                        selectedType = null;
                    }
                }
            }
            ImGui.endChild();
            ImGui.separator();
            ImGui.text("Description:");
            sectionY = (int) (DialogSize.y * DescriptionYPercentage);
            ImGui.beginChild(DescriptionSectionID, 0.0f, sectionY, ImGuiChildFlags.Borders);
            if (selectedType != null) ImGui.textWrapped(selectedType.description);
            else if (selectedCustomType != null) ImGui.textWrapped(selectedCustomType.description());
            else ImGui.textDisabled("Select an object type to see it's description.");
            ImGui.endChild();
            ImGui.separator();
            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - ImGui.getStyle().getWindowPaddingY());
            float startX = ImGui.getCursorStartPosX();
            float buttonWidth = 120.0f;
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float createX = startX + availX * 0.25f - buttonPivotX;
            float cancelX = startX + availX * 0.75f - buttonPivotX;
            ImGui.setCursorPosX(createX);
            boolean canSetType = selectedType != null || selectedCustomType != null;
            if (!canSetType) ImGui.beginDisabled();
            String label = replaceMode ? "Change" : "Create";
            if (ImGui.button(label + "##SetTypeConfirm", buttonWidth, 0.0f)) confirm();
            if (!canSetType) ImGui.endDisabled();
            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, 0)) clearDialogDataAndClose();
            ImGui.endPopup();
        }
        if (!ImGui.isPopupOpen(PopupID)) {
            showDialog = false;
            selectedType = null;
            replaceMode = false;
            targetObject = null;
        }
    }

    private static void confirm() {
        if (selectedType == null && selectedCustomType == null) return;
        if (!replaceMode) {
            if (selectedType != null) {
                GameObject root = ObjectType.getObjectFromType(selectedType);
                LogicServer.loadUnsavedScene(root);
                clearDialogDataAndClose();
                return;
            }
            try {
                GameObject root;
                try {
                    root = (GameObject) selectedCustomType.targetClass().getConstructor(String.class).newInstance(selectedCustomType.label());
                } catch (NoSuchMethodException e) {
                    root = (GameObject) selectedCustomType.targetClass().getConstructor().newInstance();
                    root.name(selectedCustomType.label());
                }
                LogicServer.loadUnsavedScene(root);
                clearDialogDataAndClose();
                return;
            } catch (Exception e) {
                EngineLog.error("Scripting", String.format("Failed to create custom object '%s': %s", selectedCustomType.label(), e.getMessage()));
                return;
            }
        }
        Scene scene = LogicServer.currentScene();
        if (scene == null) return;
        GameObject source = targetObject != null ? targetObject : scene.root();
        if (source == null) return;
        GameObject newObject = null;
        if (selectedType != null) newObject = GameObject.changeType(source, ObjectType.getClassFromType(selectedType));
        else if (selectedCustomType != null) {
            try {
                try {
                    newObject = (GameObject) selectedCustomType.targetClass().getConstructor(String.class).newInstance(selectedCustomType.label());
                } catch (NoSuchMethodException e) {
                    newObject = (GameObject) selectedCustomType.targetClass().getConstructor().newInstance();
                }
            } catch (Exception e) {
                EngineLog.error("Scripting", String.format("Failed to change to custom object type '%s': %s", selectedCustomType.label(), e.getMessage()));
                return;
            }
            newObject = GameObject.changeType(source, newObject.getClass());
        }
        if (newObject == null) return;
        SceneManager.replaceObject(scene, source, newObject);
        clearDialogDataAndClose();
    }

    private static void clearDialogDataAndClose() {
        showDialog = false;
        selectedType = null;
        selectedCustomType = null;
        replaceMode = false;
        targetObject = null;
        ImGui.closeCurrentPopup();
    }
}

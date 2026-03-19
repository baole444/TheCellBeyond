package editor.dialog;

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

public final class ChooseRootTypeDialog {
    private static final String PopupID = "Choose Root Type";
    private static final String ObjectListID = "Root_Type_List";
    private static final String DescriptionSectionID = "Root_Description_Section";
    private static final ImVec2 DialogSize = new ImVec2(600.0f, 600.0f);
    private static final float ListYPercentage = 0.6f;
    private static final float DescriptionYPercentage = 0.2f;
    private static boolean showDialog = false;
    private static boolean replaceMode = false;
    private static ObjectType selectedType = null;

    private ChooseRootTypeDialog() {}

    public static void show() {
        showDialog = true;
        replaceMode = false;
        selectedType = null;
    }

    public static void showReplace() {
        showDialog = true;
        replaceMode = true;
        selectedType = null;
    }

    public static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            String header = replaceMode ? "Select new root object type:" : "Select root object type for the new scene:";
            ImGui.text(header);
            int sectionY = (int) (DialogSize.y * ListYPercentage);
            ImGui.beginChild(ObjectListID, new ImVec2(0.0f, sectionY), ImGuiChildFlags.Border);
            for (ObjectType type : ObjectType.values()) {
                boolean isSelected = selectedType == type;
                if (ImGui.selectable(type.label + "##" + type.name(), isSelected)) selectedType = type;
            }
            ImGui.endChild();
            ImGui.separator();
            ImGui.text("Description:");
            sectionY = (int) (DialogSize.y * DescriptionYPercentage);
            ImGui.beginChild(DescriptionSectionID, new ImVec2(0.0f, sectionY), ImGuiChildFlags.Border);
            if (selectedType != null) ImGui.textWrapped(selectedType.description);
            else ImGui.textDisabled("Select an object type to see it's description.");
            ImGui.endChild();
            ImGui.separator();
            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - ImGui.getStyle().getWindowPaddingY());
            float buttonWidth = 120;
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float createX = (availX * 0.25f) - (buttonPivotX);
            float cancelX = (availX * 0.75f) - (buttonPivotX);
            ImGui.setCursorPosX(createX);
            boolean canSetType = selectedType != null;
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
        }
    }

    private static void confirm() {
        if (selectedType == null) return;
        if (!replaceMode) {
            GameObject root = ObjectType.getObjectFromType(selectedType);
            LogicServer.loadUnsavedScene(root);
            clearDialogDataAndClose();
            return;
        }
        Scene scene = LogicServer.currentScene();
        if (scene == null || scene.root() == null) return;
        GameObject newRoot = GameObject.changeType(scene.root(), ObjectType.getClassFromType(selectedType));
        if (newRoot == null) return;
        SceneManager.replaceSceneRoot(scene, newRoot);
        clearDialogDataAndClose();
    }

    private static void clearDialogDataAndClose() {
        showDialog = false;
        selectedType = null;
        replaceMode = false;
        ImGui.closeCurrentPopup();
    }
}

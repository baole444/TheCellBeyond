package editor.dialog;

import TheCellBeyond.internal.LogicServer;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import scene.SceneManager;

public final class RenameSceneDialog {
    private static final String PopupID = "Rename Scene";
    private static final ImVec2 DialogSize = new ImVec2(400.0f, 200.0f);
    private static boolean showDialog = false;
    private static boolean nameTaken = false;
    private static final ImString sceneName = new ImString(128);
    private static String errorMessage = "";

    private RenameSceneDialog() {}

    public static void show() {
        String current = LogicServer.currentSceneName();
        if (current == null) return;
        showDialog = true;
        sceneName.set(current);
        nameTaken = false;
        errorMessage = "";
    }

    public static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.spacing();
            ImGui.text("New name: ");
            ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
            if (ImGui.inputTextWithHint("##RenameSceneInput", "Enter a new name...", sceneName)) checkName();
            ImGui.popItemWidth();
            if (nameTaken || !errorMessage.isEmpty()) {
                ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.2f, 0.2f, 1.0f);
                ImGui.textWrapped(errorMessage);
                ImGui.popStyleColor(1);
            } else ImGui.text("    ");
            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();
            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - ImGui.getStyle().getWindowPaddingY());
            float buttonWidth = 120;
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float createX = (availX * 0.25f) - (buttonPivotX);
            float cancelX = (availX * 0.75f) - (buttonPivotX);
            boolean canRename = !sceneName.isEmpty() && !nameTaken && errorMessage.isEmpty();
            ImGui.setCursorPosX(createX);
            if (!canRename) ImGui.beginDisabled();
            if (ImGui.button("Rename##RenameSceneConfirm", buttonWidth, 0.0f)) rename();
            if (!canRename) ImGui.endDisabled();
            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel##RenameSceneCancel", buttonWidth, 0.0f)) {
                showDialog = false;
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }
        if (!ImGui.isPopupOpen(PopupID)) {
            showDialog = false;
            resetDialogData();
        }
    }

    private static void checkName() {
        String name = sceneName.get().trim();
        if (!SceneManager.validSceneName(name)) {
            nameTaken = false;
            errorMessage = "Name cannot be blank";
            return;
        }
        String currentName = LogicServer.currentSceneName();
        if (name.equals(currentName)) {
            nameTaken = false;
            errorMessage = "";
            return;
        }
        if (SceneManager.sceneNameTaken(name)) {
            nameTaken = true;
            errorMessage = "Scene '" + name + "' already exist";
            return;
        }
        nameTaken = false;
        errorMessage = "";
    }

    private static void rename() {
        String name = sceneName.get().trim();
        String currentName = LogicServer.currentSceneName();
        if (!SceneManager.validSceneName(name) || nameTaken) {
            errorMessage = "Entered name is empty or already taken";
            return;
        }
        if (name.equals(currentName)) {
            showDialog = false;
            ImGui.closeCurrentPopup();
            return;
        }
        if (!SceneManager.renameScene(currentName, name)) {
            errorMessage = "Failed to rename scene";
            return;
        }
        showDialog = false;
        ImGui.closeCurrentPopup();
    }

    private static void resetDialogData() {
        nameTaken = false;
        sceneName.clear();
        errorMessage = "";
    }
}

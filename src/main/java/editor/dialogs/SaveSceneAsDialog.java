package editor.dialogs;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import scene.SceneManager;

public final class SaveSceneAsDialog {
    private static final String PopupID = "Save scene as...##TCB_Save_Scene_As_Dialog";
    private static final ImVec2 DialogSize = new ImVec2(400.0f, 160.0f);
    private static final float ButtonReserve = ImGui.getFrameHeightWithSpacing();
    private static final float ButtonWidth = 100.0f;
    private static final float ButtonHeight = 30.0f;
    private static boolean showDialog = false;
    private static boolean nameTaken = false;
    private static final ImString sceneName = new ImString(128);
    private static String errorMessage = "";
    private static Runnable onSaveCallback = null;

    public static void show() {
        show(null);
    }

    public static void show(Runnable callback) {
        resetDialogData();
        showDialog = true;
        onSaveCallback = callback;
    }

    public static void imgui()  {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.spacing();
            ImGui.text("Scene's name:");
            ImGui.spacing();
            ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
            if (ImGui.inputTextWithHint("##SSAD_Scene_Name_Input", "Enter a name for the scene...", sceneName)) checkNewSceneName();
            ImGui.popItemWidth();
            if (nameTaken || !errorMessage.isEmpty()) {
                ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.2f, 0.2f, 1.0f);
                ImGui.textWrapped(errorMessage);
                ImGui.popStyleColor(1);
            } else {
                ImGui.text("    ");
            }
            ImGui.setCursorPosY(ImGui.getWindowHeight() - ButtonReserve - ButtonHeight / 2.0f);
            float buttonPivotX = ButtonWidth * 0.5f;
            float startX = ImGui.getCursorStartPosX();
            float availX = ImGui.getContentRegionAvailX();
            float saveX = startX + availX * 0.25f - buttonPivotX;
            float cancelX = startX + availX * 0.75f - buttonPivotX;
            boolean canSave = !sceneName.isEmpty() && !nameTaken && errorMessage.isEmpty();
            ImGui.setCursorPosX(saveX);
            if (!canSave) ImGui.beginDisabled();
            if (ImGui.button("Save##SSAD_Save_As_Button", ButtonWidth, ButtonHeight)) saveScene();
            if (!canSave) ImGui.endDisabled();
            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel##SSAD_Cancel_Save_Button", ButtonWidth, ButtonHeight)) {
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

    private static void resetDialogData() {
        sceneName.clear();
        nameTaken = false;
        onSaveCallback = null;
        errorMessage = "";
    }

    private static void checkNewSceneName() {
        String name = sceneName.get().trim();
        if (!SceneManager.validSceneName(name)) {
            nameTaken = false;
            errorMessage = "Name cannot be empty";
            return;
        }
        if (SceneManager.sceneNameTaken(name)) {
            nameTaken = true;
            errorMessage = "Scene '" + name + "' already existed";
            return;
        }
        nameTaken = false;
        errorMessage = "";
    }

    private static void saveScene() {
        String name = sceneName.get().trim();
        if (!SceneManager.validSceneName(name) || nameTaken) {
            errorMessage = "Entered name is empty or already taken";
            return;
        }
        if (!SceneManager.saveSceneAs(name)) {
            errorMessage = "Failed to save new scene '" + name + "'";
            return;
        }
        if (onSaveCallback != null) {
            Runnable callback = onSaveCallback;
            onSaveCallback = null;
            callback.run();
        }
        showDialog = false;
        ImGui.closeCurrentPopup();
    }

}

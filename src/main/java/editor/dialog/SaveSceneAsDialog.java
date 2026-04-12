package editor.dialog;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import scene.SceneManager;

public class SaveSceneAsDialog {
    private static final String POPUP_ID = "Save scene as...";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(400.0f, 200.0f);

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

        ImGui.openPopup(POPUP_ID);

        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;

        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DIALOG_SIZE);

        if (ImGui.beginPopupModal(POPUP_ID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.spacing();
            ImGui.text("Scene's name:");
            ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
            if (ImGui.inputTextWithHint("##SceneName", "Enter a name for the scene...", sceneName)) {
                checkNewSceneName();
            }
            ImGui.popItemWidth();

            if (nameTaken || !errorMessage.isEmpty()) {
                ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.2f, 0.2f, 1.0f);
                ImGui.textWrapped(errorMessage);
                ImGui.popStyleColor(1);
            } else {
                ImGui.text("    ");
            }
            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();
            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - ImGui.getStyle().getWindowPaddingY());

            float buttonWidth = 120;
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float saveX = (availX * 0.25f) - (buttonPivotX);
            float cancelX = (availX * 0.75f) - (buttonPivotX);

            boolean canSave = !sceneName.isEmpty() && !nameTaken && errorMessage.isEmpty();
            ImGui.setCursorPosX(saveX);
            if (canSave) {
                if (ImGui.button("Save", buttonWidth, 0)) saveScene();
            } else {
                ImGui.beginDisabled();
                ImGui.button("Save", buttonWidth, 0);
                ImGui.endDisabled();
            }
            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, 0)) {
                showDialog = false;
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }

        if (!ImGui.isPopupOpen(POPUP_ID)) {
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

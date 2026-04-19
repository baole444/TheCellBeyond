package editor.dialog;

import TheCellBeyond.internal.LogicServer;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import project.Project;
import scene.SceneManager;

public class NewSceneDialog {
    private static final String POPUP_ID = "Create new scene";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(400.0f, 200.0f);

    private static boolean showDialog = false;
    private static boolean saveCurrentScene = false;
    private static boolean nameTaken = false;
    private static final ImString sceneName = new ImString(128);

    private static String errorMessage = "";

    public static void show(boolean shouldSaveCurrentScene) {
        saveCurrentScene = shouldSaveCurrentScene;
        showDialog = true;
        sceneName.clear();
        nameTaken = false;
        errorMessage = "";
        checkNewSceneName();
    }

    public static void imgui()  {
        if (!showDialog) return;

        if (saveCurrentScene) {
            String currentSceneName = LogicServer.currentSceneName();
            if (currentSceneName != null && Project.getSceneNames().contains(currentSceneName)) {
                ConfirmSaveSceneDialog.show(() -> {
                    saveCurrentScene = false;
                }, () -> {
                    showDialog = false;
                    resetDialogData();
                });
                return;
            }
            saveCurrentScene = false;
        }

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
            float createX = (availX * 0.25f) - (buttonPivotX);
            float cancelX = (availX * 0.75f) - (buttonPivotX);

            boolean canCreate = !sceneName.isEmpty() && !nameTaken && errorMessage.isEmpty();
            ImGui.setCursorPosX(createX);
            if (canCreate) {
                if (ImGui.button("Create", buttonWidth, 0)) createNewScene();
            } else {
                ImGui.beginDisabled();
                ImGui.button("Create", buttonWidth, 0);
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
        saveCurrentScene = false;
        nameTaken = false;
        sceneName.clear();
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

    private static void createNewScene() {
        String name = sceneName.get().trim();
        if (!SceneManager.validSceneName(name) || nameTaken) {
            errorMessage = "Entered name is empty or already taken";
            return;
        }
        if (!SceneManager.createNewScene(name)) {
            errorMessage = "Failed to create new scene '" + name + "'";
            return;
        }
        SceneManager.requestLoadScene(name);
        showDialog = false;
        ImGui.closeCurrentPopup();
    }
}

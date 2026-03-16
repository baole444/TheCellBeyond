package editor;

import TheCellBeyond.internal.LogicServer;
import editor.dialog.*;
import project.Project;
import imgui.ImGui;
import scene.SceneManager;

import java.util.List;

/**
 * Menu bar of the editor.
 */
final class MenuBar {
    /**
     * Render the editor menu bar.
     */
    static void imgui() {
        if (!ImGui.beginMenuBar()) return;
        renderEditorMenu();
        renderProjectMenu();
        renderSceneMenu();
        ImGui.endMenuBar();

        EditProjectSettingsDialog.imgui();
        EditEditorPreferencesDialog.imgui();
        ExitToProjectListDialog.imgui();
        SaveSceneAsDialog.imgui();
        ConfirmSaveSceneDialog.imgui();
        NewSceneDialog.imgui();
    }

    private static void renderEditorMenu() {
        if (!ImGui.beginMenu("Editor##MenuBar_Editor_Menu")) return;
        if (ImGui.menuItem("Preferences##MenuBar_Editor_prefs")) EditEditorPreferencesDialog.show();
        if (ImGui.menuItem("Reset Layout##MenuBar_Reset_Editor_layout")) ImGuiLayer.resetLayout();
        ImGui.endMenu();
    }

    private static void renderProjectMenu() {
        if (!ImGui.beginMenu("Project##MenuBar_Project_Menu")) return;
        if (ImGui.menuItem("Preferences##MenuBar_Project_prefs")) EditProjectSettingsDialog.show();
        ImGui.separator();
        if (ImGui.menuItem("Exit to Project List##MenuBar_Exit_To_Project_List")) ExitToProjectListDialog.show();
        ImGui.endMenu();
    }

    private static void renderSceneMenu() {
        if (!ImGui.beginMenu("Scenes##MenuBar_Scene_Menu")) return;
        if (ImGui.menuItem("Save current Scene", "Ctrl+S")) SceneManager.saveCurrentScene();
        if (ImGui.menuItem("Create new Scene##MenuBar_Scene_Menu_Create_New_Scene")) showNewSceneDialog();
        ImGui.separator();
        if (!ImGui.beginMenu("Open Scene##MenuBar_Scene_Menu_Open_Scene_Menu")) {
            ImGui.endMenu();
            return;
        }
        List<String> sceneNameList = Project.getSceneNames();
        if (sceneNameList.isEmpty()) {
            ImGui.text("No scene added yet.");
            if (ImGui.menuItem("Create new Scene?##MenuBar_Scene_Menu_Ask_New_Scene")) showNewSceneDialog();
            ImGui.endMenu();
            ImGui.endMenu();
            return;
        }
        for (String name : sceneNameList) {
            if (!ImGui.menuItem(name)) continue;
            ConfirmSaveSceneDialog.show(() -> SceneManager.requestLoadScene(name));
        }
        ImGui.endMenu();
        ImGui.endMenu();
    }

    private static void showNewSceneDialog() {
        boolean requireSave = Project.loaded() && Project.getScene(LogicServer.currentSceneName()) != null;
        NewSceneDialog.show(requireSave);
    }
}


package editor;

import TheCellBeyond.internal.LogicServer;
import editor.dialog.*;
import imgui.flag.ImGuiHoveredFlags;
import project.Project;
import imgui.ImGui;
import scene.SceneManager;
import scripting.ScriptLoader;

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
        renderScriptingMenu();
        renderSceneMenu();
        ImGui.endMenuBar();

        EditProjectSettingsDialog.imgui();
        EditEditorPreferencesDialog.imgui();
        ExitToProjectListDialog.imgui();
        SaveSceneAsDialog.imgui();
        ConfirmSaveSceneDialog.imgui();
        ChooseObjectTypeDialog.imgui();
        RenameSceneDialog.imgui();
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

    private static void renderScriptingMenu() {
        if (!ImGui.beginMenu("Scripting##MenuBar_Scripting_Menu")) return;
        if (ImGui.menuItem("Reload scripts##MenuBar_Scripting_Reload_Script")) ScriptLoader.reload();
        ImGui.separator();
        if (ImGui.menuItem("Edit scan directories##MenuBar_Scripting_Edit_ScanDir")) EditProjectSettingsDialog.showToScriptTab();
        ImGui.endMenu();
    }

    private static void renderSceneMenu() {
        if (!ImGui.beginMenu("Scenes##MenuBar_Scene_Menu")) return;
        if (ImGui.menuItem("Save current Scene", "Ctrl+S")) SceneManager.saveCurrentScene();
        if (ImGui.menuItem("Create new Scene##MenuBar_Scene_Menu_Create_New_Scene")) showCreateSceneDialog();
        renderSceneEditMenu();
        ImGui.separator();
        if (!ImGui.beginMenu("Open Scene##MenuBar_Scene_Menu_Open_Scene_Menu")) {
            ImGui.endMenu();
            return;
        }
        List<String> sceneNameList = Project.getSceneNames();
        if (sceneNameList.isEmpty()) {
            ImGui.text("No scene added yet.");
            if (ImGui.menuItem("Create new Scene?##MenuBar_Scene_Menu_Ask_New_Scene")) showCreateSceneDialog();
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

    private static void renderSceneEditMenu() {
        boolean noScene = LogicServer.currentScene() == null;
        if (noScene) ImGui.beginDisabled();
        if (ImGui.beginMenu("Edit##MenuBar_Scene_Edit_Menu")) {
            boolean unsaved = LogicServer.currentSceneName() == null;
            if (unsaved) ImGui.beginDisabled();
            if (ImGui.menuItem("Rename Scene...##MenuBar_Scene_Edit_Rename")) RenameSceneDialog.show();
            if (unsaved) ImGui.endDisabled();
            if (ImGui.menuItem("Change Root Type...##MenuBar_Scene_Edit_ChangeRootType")) ChooseObjectTypeDialog.showReplaceRoot();
            ImGui.endMenu();
        }
        if (noScene && ImGui.isItemHovered(ImGuiHoveredFlags.AllowWhenDisabled)) ImGui.setTooltip("No scene loaded");
        if (noScene) ImGui.endDisabled();
    }

    private static void showCreateSceneDialog() {
        if (LogicServer.currentScene() != null) ConfirmSaveSceneDialog.show(ChooseObjectTypeDialog::show);
        else ChooseObjectTypeDialog.show();
    }
}


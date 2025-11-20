package editor;

import TheCellBeyond.Window;
import editor.dialog.*;
import project.Project;
import eventviewer.EngineEventCallback;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.ImGui;
import imgui.internal.flag.ImGuiItemFlags;

import java.util.List;

public class MenuBar {
    public static void imgui() {
        if (!ImGui.beginMenuBar()) return;
        ImGui.pushID(ImGuiItemFlags.SelectableDontClosePopup);
        if (ImGui.beginMenu("Editor")) {
            if (ImGui.menuItem("Preferences###Editor_prefs")) EditEditorPreferencesDialog.show();
            if (ImGui.menuItem("Reset Layout###Reset_Editor_layout")) ImGuiLayer.resetLayout();
            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Project")) {
            if (ImGui.menuItem("Preferences###Project_prefs")) EditProjectPreferencesDialog.show();
            ImGui.separator();
            if (ImGui.menuItem("Exit to Project List")) ExitToProjectListDialog.show();

            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Scenes")){
            if (ImGui.menuItem("Save current Scene", "Ctrl+S")) {
                EngineEventCallback.emit(null, new Event(EventType.LEVEL_SAVE));
            }

            if (ImGui.menuItem("Create new Scene")) {
                boolean requireSave = Project.getSceneNames().contains(Window.getCurrentSceneName());
                NewSceneDialog.show(requireSave);
            }
            ImGui.separator();
            if (!Project.getSceneNames().isEmpty() && ImGui.beginMenu("Open Scene")) {
                List<String> sceneNameList = Project.getSceneNames();
                for (String name : sceneNameList) {
                    if (ImGui.menuItem(name)) {
                        String sceneName = Window.getCurrentSceneName();

                        if (sceneName != null && !sceneName.equals(name) && Project.getSceneNames().contains(sceneName)) {
                            ConfirmSaveSceneDialog.show(() -> EngineEventCallback.emit(name, new Event(EventType.SCENE_LOAD)));
                        } else {
                            EngineEventCallback.emit(name, new Event(EventType.SCENE_LOAD));
                        }
                    }
                }

                ImGui.endMenu();
            }

            ImGui.endMenu();
        }

        ImGui.popID();
        ImGui.endMenuBar();

        EditProjectPreferencesDialog.imgui();
        EditEditorPreferencesDialog.imgui();
        ExitToProjectListDialog.imgui();
        SaveSceneAsDialog.imgui();
        ConfirmSaveSceneDialog.imgui();
        NewSceneDialog.imgui();
    }
}


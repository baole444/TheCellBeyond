package editor;

import TheCellBeyond.Window;
import editor.project.Project;
import eventviewer.EventSystem;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.ImGui;
import imgui.internal.flag.ImGuiItemFlags;
import scene.LevelEditorSceneInit;
import utility.ExitConfirmDialog;

import java.util.List;

import static editor.project.Project.CurrentProject;

public class MenuBar {
    private static final boolean[] mode = new boolean[] {true, false, false};
    private final ExitConfirmDialog exitConfirmDialog = new ExitConfirmDialog();

    public void imgui() {

        ImGui.beginMenuBar();
        ImGui.pushID(ImGuiItemFlags.SelectableDontClosePopup);
        if (ImGui.beginMenu("File")) {
            if (ImGui.menuItem("Save", "Ctrl+S")) {
                EventSystem.emit(null, new Event(EventType.LEVEL_SAVE));
            }

            if (ImGui.menuItem("Open", "Ctrl+O")) {
                EventSystem.emit(null, new Event(EventType.LEVEL_LOAD));
            }

            if (ImGui.menuItem("Open Project")) {
                OpenProjectDialog.openProjectDialog();
            }

            ImGui.endMenu();
        }
        if (ImGui.beginMenu("Workspace")) {
            /*
            if (ImGui.beginMenu("Editor Theme")) {
                ImGui.newLine();
                if (ImGui.menuItem(" Dark mode ", "   ", mode[1])) {
                    mode[1] = false;
                }
                if (ImGui.menuItem(" Light mode ", "   ", mode[2])) {
                    mode[2] = true;
                }
                ImGui.endMenu();
            }
             */

            if (ImGui.menuItem("Show exit confirm", exitConfirmDialog.getDialogPref())) {
                if (exitConfirmDialog.getDialogPref().equals("true")){
                    exitConfirmDialog.setDialogPref(false);
                } else if (exitConfirmDialog.getDialogPref().equals("false")) {
                    exitConfirmDialog.setDialogPref(true);
                } else {
                    exitConfirmDialog.setDialogPref(true);
                }

            }

            ImGui.endMenu();
        }

        if (CurrentProject != null && !Project.getSceneNames().isEmpty()) {
            if (ImGui.beginMenu("Scenes")){
                if (ImGui.menuItem("New scene")) {
                    // TODO: Confirm save current scene
                    Window.changeScene(new LevelEditorSceneInit());
                }

                if (ImGui.beginMenu("Select scene")) {
                    List<String> sceneNameList = Project.getSceneNames();

                    for (String name : sceneNameList) {
                        if (ImGui.menuItem(name)) {
                            EventSystem.emit(name, new Event(EventType.SCENE_LOAD));
                        }
                    }

                    ImGui.endMenu();
                }

                ImGui.endMenu();
            }

        }

        ImGui.popID();
        ImGui.endMenuBar();
    }
}


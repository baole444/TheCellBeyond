package editor;

import TCB_Field.ImGuiLayer;
import TCB_Field.Window;
import eventviewer.EventSystem;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.ImGui;
import imgui.internal.flag.ImGuiItemFlags;
import imgui.type.ImBoolean;
import scene.LevelEditorSceneInit;
import utility.ExitConfirmDialog;

import java.util.List;

import static editor.project.Project.CurrentProject;

public class MenuBar {
    private static boolean mode[] = new boolean[] {true, false, false};
    private ExitConfirmDialog exitConfirmDialog = new ExitConfirmDialog();

    public void imgui() {

        ImGui.beginMenuBar();
        ImGui.pushID(ImGuiItemFlags.SelectableDontClosePopup);
        if (ImGui.beginMenu("File")) {
            if (ImGui.menuItem("Save", "Ctrl+S")) {
                EventSystem.notice(null, new Event(EventType.LEVEL_SAVE));
            }

            if (ImGui.menuItem("Open", "Ctrl+O")) {
                EventSystem.notice(null, new Event(EventType.LEVEL_LOAD));
            }

            if (ImGui.menuItem("Open Project")) {
                ImGuiLayer.set_openFileDialog(new ImBoolean(true));
            }

            ImGui.endMenu();
        }
        if (ImGui.beginMenu("Workspace")) {
            if (ImGui.beginMenu("Editor Theme")) {
                //ImGui.text("Waiting on next ImGui fork\nto update new function\nto expose ItemFlag\nto menuItem().");
                ImGui.newLine();
                if (ImGui.menuItem(" Dark mode ", "   ", mode[1])) {
                    mode[1] = false;
                }
                if (ImGui.menuItem(" Light mode ", "   ", mode[2])) {
                    mode[2] = true;
                }
                ImGui.endMenu();
            }

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

        if (CurrentProject != null && !CurrentProject.getSceneNames().isEmpty()) {
            if (ImGui.beginMenu("Scenes")){
                if (ImGui.menuItem("New scene")) {
                    // TODO: Confirm save current scene
                    Window.changeScene(new LevelEditorSceneInit());
                }

                if (ImGui.beginMenu("Select scene")) {
                    List<String> sceneNameList = CurrentProject.getSceneNames();

                    for (String name : sceneNameList) {
                        if (ImGui.menuItem(name)) {
                            EventSystem.notice(name, new Event(EventType.SCENE_LOAD));
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


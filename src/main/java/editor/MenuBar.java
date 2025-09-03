package editor;

import TheCellBeyond.Window;
import editor.dialog.ConfirmSaveSceneDialog;
import editor.dialog.NewSceneDialog;
import editor.dialog.OpenProjectDialog;
import editor.project.Project;
import eventviewer.EngineEventCallback;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.ImGui;
import imgui.internal.flag.ImGuiItemFlags;
import utility.ExitConfirmDialog;

import java.util.List;

public class MenuBar {
    private static final boolean[] mode = new boolean[] {true, false, false};
    private final ExitConfirmDialog exitConfirmDialog = new ExitConfirmDialog();

    public void imgui() {

        ImGui.beginMenuBar();
        ImGui.pushID(ImGuiItemFlags.SelectableDontClosePopup);
        if (ImGui.beginMenu("File")) {
            if (ImGui.menuItem("Save", "Ctrl+S")) {
                EngineEventCallback.emit(null, new Event(EventType.LEVEL_SAVE));
            }

            if (ImGui.menuItem("Open", "Ctrl+O")) {
                EngineEventCallback.emit(null, new Event(EventType.LEVEL_LOAD));
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

        if (Project.currentProject() != null && !Project.getSceneNames().isEmpty()) {
            if (ImGui.beginMenu("Scenes")){
                if (ImGui.menuItem("New scene")) {
                    boolean requireSave = Project.getSceneNames().contains(Window.getCurrentSceneName());
                    NewSceneDialog.show(requireSave);
                }

                if (ImGui.beginMenu("Select scene")) {
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
        }

        ImGui.popID();
        ImGui.endMenuBar();

        NewSceneDialog.imgui();
        ConfirmSaveSceneDialog.imgui();
    }
}


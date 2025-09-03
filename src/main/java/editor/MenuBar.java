package editor;

import TheCellBeyond.Window;
import editor.dialog.ConfirmSaveSceneDialog;
import editor.dialog.NewSceneDialog;
import editor.project.Project;
import eventviewer.EngineEventCallback;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.ImGui;
import imgui.internal.flag.ImGuiItemFlags;
import utility.ExitConfirmDialog;

import java.util.List;

public class MenuBar {
    private final ExitConfirmDialog exitConfirmDialog = new ExitConfirmDialog();

    public void imgui() {
        ImGui.beginMenuBar();
        ImGui.pushID(ImGuiItemFlags.SelectableDontClosePopup);
        if (ImGui.beginMenu("Project")) {
            if (ImGui.menuItem("Edit Project Preference")) {
                System.out.println("Coming soon(tm)");
            }

            if (ImGui.menuItem("Exit to Project List")) {
                System.out.println("Coming soon(tm)");
            }

            ImGui.endMenu();
        }
        if (ImGui.beginMenu("Workspace")) {
            if (ImGui.menuItem("Toggle Exit Save", exitConfirmDialog.getDialogPref())) {
                switch (exitConfirmDialog.getDialogPref()) {
                    case "true" -> exitConfirmDialog.setDialogPref(false);
                    case null, default -> exitConfirmDialog.setDialogPref(true);
                }
            }

            ImGui.endMenu();
        }

        if (Project.currentProject() != null && !Project.getSceneNames().isEmpty()) {
            if (ImGui.beginMenu("Scenes")){
                if (ImGui.menuItem("Save current Scene", "Ctrl+S")) {
                    EngineEventCallback.emit(null, new Event(EventType.LEVEL_SAVE));
                }

                ImGui.separator();

                if (ImGui.menuItem("Create new Scene")) {
                    boolean requireSave = Project.getSceneNames().contains(Window.getCurrentSceneName());
                    NewSceneDialog.show(requireSave);
                }

                if (ImGui.beginMenu("Open Scene")) {
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


package editor;

import eventviewer.EventSystem;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.ImGui;
import imgui.flag.ImGuiSelectableFlags;
import imgui.internal.flag.ImGuiItemFlags;
import imgui.type.ImBoolean;
import utility.ExitConfirmDialog;

public class MenuBar {
    private static boolean mode[] = new boolean[] {true, false, false};
    private ExitConfirmDialog exitConfirmDialog = new ExitConfirmDialog();
    public void imgui() {

        ImGui.beginMenuBar();
        ImGui.pushID(ImGuiItemFlags.SelectableDontClosePopup);
        if (ImGui.beginMenu("File")) {
            if (ImGui.menuItem("Save", "Ctrl+S")) {
                EventSystem.notice(null, new Event(EventType.LevelSave));
            }

            if (ImGui.menuItem("Open", "Ctrl+O")) {
                EventSystem.notice(null, new Event(EventType.LevelLoad));
            }

            ImGui.endMenu();
        }
        if (ImGui.beginMenu("Workspace")) {
            if (ImGui.beginMenu("Editor Theme")) {
                ImGui.text("Waiting on next ImGui fork\nto update new function\nto expose ItemFlag\nto menuItem().");
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
        ImGui.popID();
        ImGui.endMenuBar();
    }
}


package editor;

import TCB_Field.ImGuiLayer;
import eventviewer.EventSystem;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.extension.imguifiledialog.ImGuiFileDialog;
import imgui.extension.imguifiledialog.callback.ImGuiFileDialogPaneFun;
import imgui.extension.imguifiledialog.flag.ImGuiFileDialogFlags;
import imgui.type.ImBoolean;

import java.util.Map;

public class OpenProjectDialog {
    private static final String _openProject = "open-project-key";
    private static Map<String, String> selection = null;
    private static long userData = 0;
    private static ImGuiFileDialogPaneFun callback = new ImGuiFileDialogPaneFun() {
        @Override
        public void accept(String filter, long userData, boolean canContinue) {
            //ImGui.text("Filter: " + filter);
        }
    };

    public void imgui(ImBoolean _openFileDialog) {
        if (!_openFileDialog.get()) return;

        ImGuiFileDialog.openDialog(_openProject,
                "Choose Project file", ".yml",
                ".", callback,
                250,
                1,
                42, ImGuiFileDialogFlags.DisableCreateDirectoryButton);

        if (ImGuiFileDialog.display(_openProject, ImGuiFileDialogFlags.DisableCreateDirectoryButton,
                200, 400, 800, 600)) {
            if (ImGuiFileDialog.isOk()) {
                selection = ImGuiFileDialog.getSelection();
                userData = ImGuiFileDialog.getUserDatas();
            }

            if (selection != null && !selection.isEmpty()) {
                //System.out.println("Selected: " + selection.values().stream().findFirst().get());
                //System.out.println("User data: " + userData);
                String path = selection.values().stream().findFirst().get();
                EventSystem.notice(path, new Event(EventType.LoadProject));

            }

            ImGuiLayer.set_openFileDialog(new ImBoolean(false));
            ImGuiFileDialog.close();
        }
    }
}

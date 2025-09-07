package editor;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;

public class BottomPanel {
    public static final String WINDOW_ID = "###Editor_Bottom_Panel";

    public static void imgui() {
        ImGui.begin(WINDOW_ID, ImGuiWindowFlags.NoCollapse);

        ImGui.text("Bottom panel coming soon(tm)");

        ImGui.end();
    }
}

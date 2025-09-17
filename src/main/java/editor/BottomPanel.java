package editor;

import TheCellBeyond.Window;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import render.FrameBuffer;

public class BottomPanel {
    public static final String WINDOW_ID = "###Editor_Bottom_Panel";

    public static void imgui() {
        ImGui.begin(WINDOW_ID, ImGuiWindowFlags.NoCollapse);
        ImGui.text("Bottom panel coming soon(tm)");
        ImGui.end();
    }
}

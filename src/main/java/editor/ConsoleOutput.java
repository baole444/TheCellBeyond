package editor;

import TheCellBeyond.ConsoleStream;
import imgui.ImGui;

public class ConsoleOutput {
    public static void imgui() {
        ImGui.begin("Output###editor_console_out");

        for (String line : ConsoleStream.get().getLines()) {
            ImGui.textUnformatted(line);
        }

        if (ImGui.getScrollY() >= ImGui.getScrollMaxX()) ImGui.setScrollHereY(1.0f);

        ImGui.end();
    }
}

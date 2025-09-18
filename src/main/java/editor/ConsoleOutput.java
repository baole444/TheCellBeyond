package editor;

import TheCellBeyond.ConsoleStream;
import imgui.ImGui;

public class ConsoleOutput {
    static void imgui() {
        for (String line : ConsoleStream.get().getLines()) {
            ImGui.textUnformatted(line);
        }

        if (ImGui.getScrollY() >= ImGui.getScrollMaxX()) ImGui.setScrollHereY(1.0f);
    }
}

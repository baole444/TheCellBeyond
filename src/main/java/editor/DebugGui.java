package editor;

import imgui.ImGui;

public class DebugGui {
    private float[] printDebug;

    private boolean[] showContent = new boolean[] {false, false, false, false, false, false};
    public void imgui(){
        printDebug = GameViewPort.debugOutput();
        ImGui.begin("Debug Viewport stat");

        if (ImGui.button("Statistic")) {
            showContent[0] = true;
        }

        if (showContent[0]) {
            ImGui.newLine();
            ImGui.text("Viewport size (x | y) : " + printDebug[0] + " | " + printDebug[1]);
            ImGui.text("Viewport position:");
            ImGui.text("    Centre (x | y): " + printDebug[2] + " | " + printDebug[3]);
            ImGui.text("    x (L | R): " + printDebug[3] + " | " + printDebug[4]);
            ImGui.text("    y (B | T): " + printDebug[5] + " | " + printDebug[6]);

            if (ImGui.button("Hide statistic")) {
                showContent[0] = false;
            }
        }

        ImGui.newLine();

        if (ImGui.button("Debug draw tool")) {
            showContent[1] = true;
        }

        if (showContent[1]) {
            ImGui.text(" \n");
            ImGui.text("... No test tool added yet :<");
            ImGui.text(" \n");

            if (ImGui.button("Hide tool")) {
                showContent[1] = false;
            }
        }

        ImGui.end();
    }
}

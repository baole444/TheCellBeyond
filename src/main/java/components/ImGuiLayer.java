package components;

import imgui.ImGui;

public class ImGuiLayer {
    private boolean showText = false;

    public void imgui() {
        ImGui.begin("Test");

        if (ImGui.button("test button")) {
            showText = true;
        }

        if (showText) {
            ImGui.text("button registered");
            ImGui.sameLine();
            if (ImGui.button("End text")) {
                showText = false;
            }
        }

        ImGui.end();
    }

}

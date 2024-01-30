package editor;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class ImEditorGui {
    private static float defaultWidth = 90.0f;
    public static void drawVec2Ctrl(String label, Vector2f val) {
        drawVec2Ctrl(label, val, 0.0f, defaultWidth);
    }

    public static void drawVec2Ctrl(String label, Vector2f val, float resetVal) {
        drawVec2Ctrl(label, val, resetVal, defaultWidth);
    }
    public static void drawVec2Ctrl(String label, Vector2f val, float resetVal, float columnWidth) {
        ImGui.pushID(label);

        ImGui.columns(2);
        ImGui.setColumnWidth(0, columnWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        float lineHeight = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 2.0f;
        Vector2f labelSize = new Vector2f(lineHeight + 3.0f, lineHeight);
        float remainWidth = (ImGui.calcItemWidth() - labelSize.x * 2.0f) / 2.0f;

        //=================== x button ===================
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button("x", labelSize.x, labelSize.y)) {
            val.x = resetVal;
        }
        ImGui.popStyleColor(3);
        ImGui.sameLine();
        float[] valX = {val.x};
        ImGui.dragFloat("##x", valX, 1f);
        ImGui.popItemWidth();
        ImGui.sameLine();
        //================================================

        //=================== y button ===================
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.8f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.7f, 0.2f, 1.0f);
        if (ImGui.button("y", labelSize.x, labelSize.y)) {
            val.y = resetVal;
        }
        ImGui.popStyleColor(3);
        ImGui.sameLine();
        float[] valY = {val.y};
        ImGui.dragFloat("##y", valY, 1f);
        ImGui.popItemWidth();
        ImGui.sameLine();
        //================================================


        ImGui.nextColumn();

        // Update value here
        val.x = valX[0];
        val.y = valY[0];


        // End and reset
        ImGui.popStyleVar();
        ImGui.columns(1);
        ImGui.popID();
    }

    public static float dragFloatCtrl(String label, float val) {
        ImGui.pushID(label);

        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        float[] valA = {val};
        ImGui.dragFloat("##dragFloat", valA, 0.1f);

        // End and reset
        ImGui.columns(1);
        ImGui.popID();

        return valA[0];
    }

    public static int dragIntCtrl(String label, int val) {
        ImGui.pushID(label);

        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        int[] valA = {val};
        ImGui.dragInt("##dragInt", valA, 1);

        // End and reset
        ImGui.columns(1);
        ImGui.popID();

        return valA[0];
    }

    public static boolean colorCtrl(String label, Vector4f val) {
        boolean result = false;
        ImGui.pushID(label);

        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        float[] color = {val.x, val.y, val.z, val.w};
        if(ImGui.colorEdit4("##color", color)) {
            val.set(color[0], color[1], color[2], color[3]);
            result = true;
        }

        // End and reset
        ImGui.columns(1);
        ImGui.popID();

        return result;
    }
}

package editor;

import TCB_Field.KeyListener;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import org.joml.Vector2f;
import org.joml.Vector4f;

import static org.lwjgl.glfw.GLFW.*;

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

    public static void spriteKeyTransform(String label, Vector2f val, int step) {
        ImGui.pushID(label);
        ImGui.newLine();
        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultWidth * 1.25f);
        ImGui.nextColumn();

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        float lineHeight = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 2.0f;
        Vector2f labelSize = new Vector2f(lineHeight + 2.0f, lineHeight);
        float remainWidth = (ImGui.calcItemWidth() - labelSize.x * 2.0f) / 2.0f;

        //=================== up ===================
        ImGui.nextColumn();
        ImGui.nextColumn();
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.8f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.invisibleButton("empty", 90.0f, labelSize.y);
        ImGui.sameLine();

        if (ImGui.button("  Up  ", 80.0f, labelSize.y) || KeyListener.isKeyPressed(GLFW_KEY_UP)) {
            val.y += step;
        }

        ImGui.sameLine();
        ImGui.invisibleButton("empty", 90.0f, labelSize.y);
        ImGui.popStyleColor(3);
        ImGui.popItemWidth();
        ImGui.newLine();
        //================================================

        //=================== Left central Right ===================
        ImGui.nextColumn();
        ImGui.text(label);
        ImGui.nextColumn();
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);

        if (ImGui.button(" Left ", 80.0f, labelSize.y) || KeyListener.isKeyPressed(GLFW_KEY_LEFT)) {
            val.x -= step;
        }
        ImGui.popStyleColor(3);

        ImGui.sameLine();
        ImGui.invisibleButton("empty", 10.0f, labelSize.y);
        ImGui.sameLine();

        ImGui.pushStyleColor(ImGuiCol.Button, 0.6f, 0.25f, 0.0f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.75f, 0.31f, 0.0f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.6f, 0.25f, 0.0f, 1.0f);

        if (ImGui.button("Nearest", 80.0f, labelSize.y) || KeyListener.isKeyPressed(GLFW_KEY_N)) {
            while (val.x % 32.0f != 0.0f || val.y % 32.0f != 0.0f) {
                float offsetX = val.x % 32.0f;
                float offsetY = val.y % 32.0f;
                if (offsetX != 0 && Math.abs(offsetX) >= 16.0f) {
                    val.x += offsetX;
                } else if (offsetX != 0 && Math.abs(offsetX) < 16.0f) {
                    val.x -= offsetX;
                } else {
                    val.x += 0;
                }

                if (offsetY != 0 && Math.abs(offsetY) >= 16.0f) {
                    val.y += offsetY;
                } else if (offsetY != 0 && Math.abs(offsetY) < 16.0f) {
                    val.y -= offsetY;
                } else {
                    val.y += 0;
                }
            }
        }
        ImGui.popStyleColor(3);

        ImGui.sameLine();
        ImGui.invisibleButton("empty", 10.0f, labelSize.y);
        ImGui.sameLine();

        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);

        if (ImGui.button(" Right ", 80.0f, labelSize.y) || KeyListener.isKeyPressed(GLFW_KEY_RIGHT)) {
            val.x += step;
        }

        ImGui.popStyleColor(3);
        ImGui.popItemWidth();
        ImGui.newLine();
        //================================================

        //=================== down ===================
        ImGui.nextColumn();
        ImGui.nextColumn();
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.8f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.invisibleButton("empty", 90.0f, labelSize.y);
        ImGui.sameLine();
        if (ImGui.button(" Down ", 80.0f, labelSize.y) || KeyListener.isKeyPressed(GLFW_KEY_DOWN)) {
            val.y -= step;
        }

        ImGui.sameLine();
        ImGui.invisibleButton("empty", 90.0f, labelSize.y);
        ImGui.popStyleColor(3);
        ImGui.popItemWidth();


        ImGui.nextColumn();



        // End and reset
        ImGui.popStyleVar();
        ImGui.columns(1);
        ImGui.popID();
        ImGui.newLine();
    }

    public static float dragFloatCtrl(String label, float val) {
        ImGui.pushID(label);

        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        float lineHeight = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 2.0f;
        Vector2f labelSize = new Vector2f(lineHeight * 2.4f, lineHeight);
        float remainWidth = (ImGui.calcItemWidth() - labelSize.x);

        //=================== reset button ===================
        ImGui.pushItemWidth(remainWidth);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button("Reset", labelSize.x, labelSize.y)) {
            val = 0.0f;
        }
        ImGui.popStyleColor(3);
        ImGui.sameLine();
        float[] valA = {val};
        ImGui.dragFloat("##dragFloat", valA, 1.0f);
        ImGui.popItemWidth();
        ImGui.sameLine();
        //================================================

        ImGui.nextColumn();

        // End and reset
        ImGui.popStyleVar();
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

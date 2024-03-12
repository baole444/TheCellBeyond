package editor;

import TCB_Field.MouseListener;
import imgui.ImGui;
import org.joml.Vector2f;
import org.joml.Vector3f;
import render.DebugDraw;

public class DebugGui {
    private float[] printDebug;
    private int x, y;
    private float wX, wY;

    private boolean[] showContent = new boolean[] {false, false, false, false, false, false};
    public void imgui(){
        printDebug = GameViewPort.debugOutput();
        ImGui.begin("Debug Viewport stat");

        if (ImGui.button("Statistic")) {
            showContent[0] = true;
        }

        if (showContent[0]) {
            ImGui.sameLine();
            if (ImGui.button("Hide statistic")) {
                showContent[0] = false;
            }
            ImGui.newLine();
            ImGui.text("Viewport size (x | y) : " + printDebug[0] + " | " + printDebug[1]);
            ImGui.text("Viewport position:");
            ImGui.text("    Centre (x | y): " + printDebug[2] + " | " + printDebug[3]);
            ImGui.text("    x (L | R): " + printDebug[4] + " | " + printDebug[5]);
            ImGui.text("    y (B | T): " + printDebug[6] + " | " + printDebug[7]);

        }


        if (ImGui.button("Debug tool")) {
            showContent[1] = true;
        }

        if (showContent[1]) {
            ImGui.sameLine();
            if (ImGui.button("Hide tool")) {
                showContent[1] = false;
            }
            ImGui.newLine();
            ImGui.text("  Draw commands:");
            ImGui.newLine();


            ImGui.text("    Circle:");
            ImGui.sameLine();
            if (ImGui.button("Central")) {
                DebugDraw.addCircle(new Vector2f(3.2f, 2.4f), 1, new Vector3f(1, 1, 1), 300);
            }

            ImGui.text("    Lines:");
            ImGui.sameLine();
            if (ImGui.button("Screen crossing")) {
                DebugDraw.addLine2(new Vector2f(0, 0), new Vector2f(6.4f, 4.8f), new Vector3f(1, 1, 1), 300);
                DebugDraw.addLine2(new Vector2f(0, 4.8f), new Vector2f(6.4f, 0), new Vector3f(1, 1, 1), 300);
            }

            ImGui.newLine();

            ImGui.text("    Corners:");
            ImGui.sameLine();
            if (ImGui.button("Screen corners")) {
                DebugDraw.addCircle(new Vector2f(0, 0), 1, new Vector3f(1, 1, 1), 300);
                DebugDraw.addCircle(new Vector2f(0, 4.8f), 1, new Vector3f(1, 1, 1), 300);
                DebugDraw.addCircle(new Vector2f(6.4f, 4.8f), 1, new Vector3f(1, 1, 1), 300);
                DebugDraw.addCircle(new Vector2f(6.4f, 0), 1, new Vector3f(1, 1, 1), 300);
                DebugDraw.addLine2(new Vector2f(0, 0), new Vector2f(0, 4.8f), new Vector3f(1, 1, 1), 300);
                DebugDraw.addLine2(new Vector2f(0, 0), new Vector2f(6.4f, 0), new Vector3f(1, 1, 1), 300);
                DebugDraw.addLine2(new Vector2f(6.4f, 0), new Vector2f(6.4f, 4.8f), new Vector3f(1, 1, 1), 300);
                DebugDraw.addLine2(new Vector2f(6.4f, 4.8f), new Vector2f(0, 4.8f), new Vector3f(1, 1, 1), 300);
            }

            ImGui.newLine();

            ImGui.text("  Callback commands:");
            ImGui.newLine();

            //Cursor call back position

            ImGui.text("    Cursor register:");
            ImGui.sameLine();

            if (ImGui.button("Show")) {
                showContent[2] = true;
            }
            ImGui.sameLine();
            if (showContent[2]) {
                if (ImGui.button("Hide")) {
                    x = 0;
                    y = 0;
                    showContent[2] = false;
                }

                x = (int)MouseListener.loadScrX();
                y = (int)MouseListener.loadScrY();
                wX = MouseListener.getWorldX();
                wY = MouseListener.getWorldY();
                DebugDraw.addCircle(new Vector2f(wX, wY), 0.5f, new Vector3f(1, 1, 1), 1);
                ImGui.text("    x " + x);
                ImGui.text("    y " + y);
                ImGui.text("    worldX " + wX);
                ImGui.text("    worldY " + wY);
                ImGui.text("    Is Dragging:");
                ImGui.sameLine();
                if (MouseListener.isDragging()) {
                    ImGui.text("TRUE");
                } else {
                    ImGui.text("FALSE");
                }

            }


        }
        ImGui.end();
    }
}

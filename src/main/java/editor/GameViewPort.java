package editor;

import TCB_Field.MouseListener;
import TCB_Field.Window;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiWindowFlags;
import org.joml.Vector2f;

public class GameViewPort {
    private float leftX, rightX, topY, bottomY;
    private static float[] printDebug;
    public void imgui() {
        // TODO: BROKEN COORDINATE, WILL NEED TO FIX FOR CURSOR LIMITATION WILL WORK
        ImGui.begin("Game Viewport", ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

        ImVec2 winSize = loadMaxViewportSize();
        ImVec2 winPos = loadViewportToCentral(winSize);

        ImGui.setCursorPos(winPos.x, winPos.y);

        ImVec2 topLeft = new ImVec2();
        ImGui.getCursorScreenPos(topLeft);
        topLeft.x -= ImGui.getScrollX();
        topLeft.y -= ImGui.getScrollY();
        leftX = topLeft.x;
        topY =  topLeft.y;
        rightX = topLeft.x + winSize.x;
        bottomY = winSize.y - topY;

        this.printDebug = new float[] {winSize.x, winSize.y,winPos.x, winPos.y, leftX, rightX, bottomY, topY};

        int texID = Window.loadFrameBuffer().loadTexID();

        ImGui.image(texID, winSize.x, winSize.y, 0, 1, 1, 0);

        MouseListener.setWorkViewportPos(new Vector2f(topLeft.x, topLeft.y));
        MouseListener.setWorkViewportSize(new Vector2f(winSize.x, winSize.y));

        ImGui.end();
    }

    public static float[] debugOutput() {
        return printDebug;
    }

    public boolean getWantCaptureMouse() {
        return MouseListener.getX() >= leftX &&
                MouseListener.getX() <= rightX &&
                MouseListener.getY() <= bottomY &&
                MouseListener.getY() >= topY;

    }

    private ImVec2 loadMaxViewportSize() {
        ImVec2 winSize = new ImVec2();
        ImGui.getContentRegionAvail(winSize);
        winSize.x -= ImGui.getScrollX();
        winSize.y -= ImGui.getScrollY();

        float usableWidth = winSize.x;
        float usableHeight = usableWidth / Window.loadTargetAspectRatio();
        if (usableHeight > winSize.y) {
            // Generate black region
            usableHeight = winSize.y;
            usableWidth = usableHeight * Window.loadTargetAspectRatio();
        }

        return new ImVec2(usableWidth, usableHeight);
    }

    private ImVec2 loadViewportToCentral(ImVec2 usableSize) {
        ImVec2 winSize = new ImVec2();
        ImGui.getContentRegionAvail(winSize);
        winSize.x -= ImGui.getScrollX();
        winSize.y -= ImGui.getScrollY();

        float portX = (winSize.x / 2.0f) - (usableSize.x / 2.0f);
        float portY = (winSize.y / 2.0f) - (usableSize.y / 2.0f);

        return new ImVec2(portX + ImGui.getCursorPosX(), portY + ImGui.getCursorPosY());
    }

}

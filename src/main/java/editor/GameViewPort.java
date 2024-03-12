package editor;

import TCB_Field.MouseListener;
import TCB_Field.Window;
import eventviewer.EventSystem;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiWindowFlags;
import org.joml.Vector2f;

public class GameViewPort {
    private float leftX, rightX, topY, bottomY;
    private static float[] printDebug;
    private boolean isBegun = false;
    public void imgui() {

        ImGui.begin("Game Viewport", ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse | ImGuiWindowFlags.MenuBar);

        ImGui.beginMenuBar();

        if (ImGui.menuItem("Play", "", isBegun, !isBegun)) {
            isBegun = true;
            EventSystem.notice(null, new Event(EventType.EngineStart));
        }

        if (ImGui.menuItem("Stop", "", !isBegun, isBegun)) {
            isBegun = false;
            EventSystem.notice(null, new Event(EventType.EngineEnd));
        }

        ImGui.endMenuBar();

        ImGui.setCursorPos(ImGui.getCursorPosX(), ImGui.getCursorPosY());
        ImVec2 winSize = loadMaxViewportSize();
        ImVec2 winPos = loadViewportToCentral(winSize);
        ImGui.setCursorPos(winPos.x, winPos.y);

        leftX = winPos.x + ImGui.getWindowPosX();
        rightX = winPos.x + winSize.x + ImGui.getWindowPosX();
        bottomY =  winPos.y;
        topY = winPos.y + winSize.y;

        this.printDebug = new float[] {winSize.x, winSize.y,winPos.x, winPos.y, leftX, rightX, bottomY, topY};

        int texID = Window.loadFrameBuffer().loadTexID();

        ImGui.image(texID, winSize.x, winSize.y, 0, 1, 1, 0);

        MouseListener.setWorkViewportPos(new Vector2f(winPos.x + ImGui.getWindowPosX(), winPos.y));
        MouseListener.setWorkViewportSize(new Vector2f(winSize.x, winSize.y));

        ImGui.end();
    }

    public static float[] debugOutput() {
        return printDebug;
    }

    public boolean getWantCaptureMouse() {
        return MouseListener.getX() >= leftX &&
                MouseListener.getX() <= rightX &&
                MouseListener.getY() >= bottomY &&
                MouseListener.getY() <= topY;

    }

    private ImVec2 loadMaxViewportSize() {
        ImVec2 winSize = new ImVec2();
        ImGui.getContentRegionAvail(winSize);

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

        float portX = (winSize.x / 2.0f) - (usableSize.x / 2.0f);
        float portY = (winSize.y / 2.0f) - (usableSize.y / 2.0f);

        return new ImVec2(portX + ImGui.getCursorPosX(), portY + ImGui.getCursorPosY());
    }

}

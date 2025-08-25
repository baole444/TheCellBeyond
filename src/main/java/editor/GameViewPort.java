package editor;

import TheCellBeyond.MouseListener;
import TheCellBeyond.Window;
import editor.project.ProjectPreference;
import eventviewer.EventSystem;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiPopupFlags;
import imgui.flag.ImGuiWindowFlags;
import org.joml.Vector2f;
import render.FrameBuffer;

public class GameViewPort {
    private float leftX, rightX, topY, bottomY;
    private static float[] printDebug;
    private boolean isPlaying = false;

    public void imgui() {
        ImGui.begin("Game Viewport", ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoScrollWithMouse
                | ImGuiWindowFlags.MenuBar
        );

        // Create menu bar
        ImGui.beginMenuBar();
        if (ImGui.menuItem("Play","", isPlaying, !isPlaying)) {
            isPlaying = true;
            EventSystem.emit(null, new Event(EventType.ENGINE_START));
        }

        if (ImGui.menuItem("Stop","", !isPlaying, isPlaying)) {
            isPlaying = false;
            EventSystem.emit(null, new Event(EventType.ENGINE_END));
        }

        ImGui.endMenuBar();
        // End menu bar

        ImVec2 winSize = ImGui.getContentRegionAvail();
        ImVec2 winPos = ImGui.getCursorPos();

        leftX = winPos.x;
        rightX = winPos.x + winSize.x;
        bottomY =  winPos.y;
        topY = winPos.y + winSize.y;

        printDebug = new float[] {winSize.x, winSize.y, winPos.x, winPos.y, leftX, rightX, bottomY, topY};


        FrameBuffer frameBuffer = Window.getFrameBuffer();
        int texID = frameBuffer.getTextureID();

        ImGui.image(texID, frameBuffer.getWidth(), frameBuffer.getHeight(), 0, 1, 1, 0);

        MouseListener.setWorkViewportPos(new Vector2f(winPos.x, winPos.y));
        MouseListener.setWorkViewportSize(new Vector2f(winSize.x, winSize.y));

        ImGui.end();
    }

    public static float[] debugOutput() {
        return printDebug;
    }

    public boolean getWantCaptureMouse() {
        if (ImGui.isPopupOpen("", ImGuiPopupFlags.AnyPopup)) return false;

        return MouseListener.getX() >= leftX &&
                MouseListener.getX() <= rightX &&
                MouseListener.getY() >= bottomY &&
                MouseListener.getY() <= topY;

    }
}

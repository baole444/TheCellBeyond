package editor;

import TheCellBeyond.MouseListener;
import TheCellBeyond.Window;
import editor.project.Project;
import eventviewer.EngineEventCallback;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiPopupFlags;
import imgui.flag.ImGuiWindowFlags;
import org.joml.Vector2f;

public class SceneEditorViewport {
    private float leftX, rightX, topY, bottomY;
    private static float[] printDebug;
    private boolean isPlaying = false;
    private String currentSceneName = "New scene";
    public void imgui() {
        String sceneName = Window.getCurrentSceneName();

        if (sceneName != null && !currentSceneName.equals(sceneName)) {
            currentSceneName = sceneName;
        }

        ImGui.begin(currentSceneName + "###Game Viewport", ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoScrollWithMouse
                | ImGuiWindowFlags.MenuBar
        );

        ImGui.beginMenuBar();
        if (ImGui.menuItem("Play","", isPlaying, !isPlaying)) {
            isPlaying = true;
            EngineEventCallback.emit(null, new Event(EventType.ENGINE_START));
        }

        if (ImGui.menuItem("Stop","", !isPlaying, isPlaying)) {
            isPlaying = false;
            EngineEventCallback.emit(null, new Event(EventType.ENGINE_END));
        }

        ImGui.endMenuBar();

        ImGui.setCursorPos(ImGui.getCursorPosX(), ImGui.getCursorPosY());

        ImVec2 winSize = getMaxViewportSize();
        ImVec2 winPos = getViewportToCentral(winSize);

        ImGui.setCursorPos(winPos.x, winPos.y);

        ImVec2 topLeft = ImGui.getCursorScreenPos();
        topLeft.x -= ImGui.getScrollX();
        topLeft.y -= ImGui.getScrollY();
        leftX = winPos.x + ImGui.getWindowPosX();
        rightX = winPos.x + winSize.x + ImGui.getWindowPosX();
        bottomY =  winPos.y + ImGui.getWindowPosY();
        topY = winPos.y + winSize.y + ImGui.getWindowPosY();

        printDebug = new float[] {winSize.x, winSize.y, winPos.x, winPos.y, leftX, rightX, bottomY, topY};

        int texID = Window.getFrameBuffer().getTextureID();

        ImGui.image(texID, winSize.x, winSize.y, 0, 1, 1, 0);

        MouseListener.setWorkViewportPos(new Vector2f(leftX, bottomY));
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

    private ImVec2 getMaxViewportSize() {
        ImVec2 winSize = new ImVec2();
        ImGui.getContentRegionAvail(winSize);

        float aspectRatio = Window.get().isRuntimeMode() ?
                Project.getGameAspectRatio() :
                Window.getScene().viewport().getAspectRatio();

        float usableWidth = winSize.x;
        float usableHeight = usableWidth / aspectRatio;
        if (usableHeight > winSize.y) {
            usableHeight = winSize.y;
            usableWidth = usableHeight * aspectRatio;
        }

        return new ImVec2(usableWidth, usableHeight);
    }

    private ImVec2 getViewportToCentral(ImVec2 usableSize) {
        ImVec2 winSize = new ImVec2();
        ImGui.getContentRegionAvail(winSize);

        float portX = (winSize.x / 2.0f) - (usableSize.x / 2.0f);
        float portY = (winSize.y / 2.0f) - (usableSize.y / 2.0f);

        return new ImVec2(portX + ImGui.getCursorPosX(), portY + ImGui.getCursorPosY());
    }

}

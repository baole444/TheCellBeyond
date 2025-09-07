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
    public static volatile String WINDOW_ID = "2D Scene###Editor_Scene_Viewport";
    private float leftX, rightX, topY, bottomY;
    private boolean isPlaying = false;
    private String currentSceneName = "New scene";

    public void imgui() {
        String sceneName = Window.getCurrentSceneName();

        if (sceneName == null) currentSceneName = "Untitled";
        if (sceneName != null && !currentSceneName.equals(sceneName)) currentSceneName = sceneName;
        ImGui.begin(WINDOW_ID, ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoScrollWithMouse
                | ImGuiWindowFlags.MenuBar
                | ImGuiWindowFlags.NoCollapse
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

        float remainWidth = ImGui.getContentRegionAvailX();
        float textWidth = ImGui.calcTextSizeX(currentSceneName);
        float offset = Math.max((remainWidth - textWidth) * 0.5f, 0.0f);
        ImGui.setCursorPosX(ImGui.getCursorPosX() + offset);
        ImGui.text(currentSceneName);

        ImGui.endMenuBar();

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

        int texID = Window.getFrameBuffer().getTextureID();

        ImGui.image(texID, winSize.x, winSize.y, 0, 1, 1, 0);

        MouseListener.setWorkViewportPos(new Vector2f(leftX, bottomY));
        MouseListener.setWorkViewportSize(new Vector2f(winSize.x, winSize.y));

        ImGui.end();
    }

    public boolean getWantCaptureMouse() {
        if (ImGui.isPopupOpen("", ImGuiPopupFlags.AnyPopup)) return false;

        return MouseListener.getX() >= leftX &&
                MouseListener.getX() <= rightX &&
                MouseListener.getY() >= bottomY &&
                MouseListener.getY() <= topY;

    }

    private ImVec2 getMaxViewportSize() {
        ImVec2 winSize = ImGui.getContentRegionAvail();

        float aspectRatio = Window.get().isRuntimeMode() ?
                Project.getGameAspectRatio() :
                (float) Window.getWidth() / Window.getHeight();

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

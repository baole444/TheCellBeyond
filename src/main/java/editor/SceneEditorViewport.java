package editor;

import TheCellBeyond.MouseListener;
import TheCellBeyond.Window;
import TheCellBeyond.internal.LogicServer;
import editor.dialog.SaveSceneAsDialog;
import editor.preference.EditorPreferences;
import editor.preference.UserPreference;
import eventviewer.EngineEventListener;
import eventviewer.event.RuntimeEvent;
import eventviewer.event.Event;
import project.Project;
import eventviewer.EngineEventCallback;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiPopupFlags;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import org.joml.Vector2f;
import render.FrameBuffer;

public class SceneEditorViewport implements EngineEventListener {
    public static volatile String WINDOW_ID = "2D Scene###Editor_Scene_Viewport";
    private float leftX, rightX, topY, bottomY;
    private boolean isPlaying = false;
    private String currentSceneName = "New scene";
    public transient float currentWidth;
    public transient float currentHeight;

    SceneEditorViewport() {
        EngineEventCallback.register(this);
    }

    public void imgui() {
        String sceneName = LogicServer.currentSceneName();

        if (sceneName == null) currentSceneName = "Untitled";
        if (sceneName != null && !currentSceneName.equals(sceneName)) currentSceneName = sceneName;
        if (!ImGui.begin(WINDOW_ID, ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoScrollWithMouse
                | ImGuiWindowFlags.MenuBar
                | ImGuiWindowFlags.NoCollapse
        )) {
            ImGui.end();
            return;
        }

        if (!ImGui.beginMenuBar()) {
            ImGui.end();
            return;
        }

        if (!ImGui.beginTable("##ESV_MenuBar_Table_Div", 3, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingFixedFit)) {
            ImGui.endMenuBar();
            ImGui.end();
            return;
        }
        ImGui.tableSetupColumn("##Runtime_switch_column_ESV", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("Scene_name_column_ESV", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("Scene_Editing_Controls_ESV", ImGuiTableColumnFlags.WidthFixed);

        ImGui.tableNextColumn();
        if (ImGui.menuItem("Play","", isPlaying, !isPlaying)) {
            if (LogicServer.currentSceneName() == null) {
                SaveSceneAsDialog.show(() -> {
                    isPlaying = true;
                    EngineEventCallback.emit(null, new RuntimeEvent(RuntimeEvent.Type.RuntimeStarted));
                });
            } else {
                isPlaying = true;
                EngineEventCallback.emit(null, new RuntimeEvent(RuntimeEvent.Type.RuntimeStarted));
            }
        }

        if (ImGui.menuItem("Stop","", !isPlaying, isPlaying)) {
            isPlaying = false;
            EngineEventCallback.emit(null, new RuntimeEvent(RuntimeEvent.Type.RuntimeStopped));
        }

        ImGui.sameLine();
        ImGui.text(" ");

        ImGui.tableNextColumn();
        EditorWidget.textCenterAlign(currentSceneName);

        ImGui.tableNextColumn();
        ImBoolean snapGrid = new ImBoolean(UserPreference.editorPreferences().showGridLine());
        if (ImGui.checkbox("Grid snapping##Ctrl_Grid_Snap_nd_Show_ESV", snapGrid)) {
            boolean enable = snapGrid.get();
            EditorPreferences currentPrefs = UserPreference.editorPreferences();
            EditorPreferences newPrefs = new EditorPreferences(currentPrefs.autoSaveOnExit(), currentPrefs.autoSaveOnChangeScene(), enable);
            UserPreference.updateEditorPreferences(newPrefs);
        }

        ImGui.endTable();
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

        MouseListener.setCurrentViewportPosition(new Vector2f(leftX, bottomY));
        MouseListener.setCurrentViewportSize(new Vector2f(winSize.x, winSize.y));

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
        FrameBuffer fb = Window.getFrameBuffer();
        float aspectRatio = LogicServer.runtimeMode() ?
                Project.getGameAspectRatio() :
                (float) fb.getWidth() / fb.getHeight();

        float usableWidth = winSize.x;
        float usableHeight = usableWidth / aspectRatio;
        if (usableHeight > winSize.y) {
            usableHeight = winSize.y;
            usableWidth = usableHeight * aspectRatio;
        }

        if (usableWidth != currentWidth || usableHeight != currentHeight) {
            currentWidth = usableWidth;
            currentHeight = usableHeight;
        }

        if (!LogicServer.runtimeMode()) {
            LogicServer.currentScene().viewport().adjustSceneScale(usableHeight);
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

    @Override
    public void onEventEmit(Object object, Event event) {
        if (!(event instanceof RuntimeEvent runtimeEvent)) return;
        switch (runtimeEvent.type) {
            case RuntimeStopped, RuntimeCrashed -> isPlaying = false;
            case RuntimeStarted -> isPlaying = true;
        }
    }
}

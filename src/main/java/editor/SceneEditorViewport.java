package editor;

import TheCellBeyond.MouseListener;
import TheCellBeyond.Viewport;
import TheCellBeyond.Window;
import TheCellBeyond.internal.LogicServer;
import editor.dialog.SaveSceneAsDialog;
import editor.preference.EditorPreferences;
import editor.preference.UserPreference;
import eventviewer.EngineEventListener;
import eventviewer.event.RuntimeEvent;
import eventviewer.event.Event;
import eventviewer.event.SceneEvent;
import imgui.flag.*;
import imgui.type.ImString;
import project.Project;
import eventviewer.EngineEventCallback;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.type.ImBoolean;
import org.joml.Vector2f;
import render.FrameBuffer;
import scene.SceneManager;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

public class SceneEditorViewport implements EngineEventListener {
    public static volatile String WINDOW_ID = "2D Scene###Editor_Scene_Viewport";
    private float leftX, rightX, topY, bottomY;
    private boolean isPlaying = false;
    private String currentSceneName = null;
    private boolean renaming = false;
    private boolean renameFocus = false;
    private final ImString renameBuffer = new ImString(128);
    public transient float currentWidth;
    public transient float currentHeight;

    SceneEditorViewport() {
        register();
    }

    public void imgui() {
        currentSceneName = resolveDisplaySceneName();
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
        renderSceneName();
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
        ImGui.beginGroup();
        ImVec2 cursorPos = ImGui.getCursorPos();
        ImGui.image(texID, winSize.x, winSize.y, 0, 1, 1, 0);
        renderFPS(cursorPos);
        ImGui.endGroup();
        MouseListener.setCurrentViewportPosition(new Vector2f(leftX, bottomY));
        MouseListener.setCurrentViewportSize(new Vector2f(winSize.x, winSize.y));
        ImGui.end();
    }

    private void renderFPS(ImVec2 cursorPos) {
        String fps = String.format("%.2f FPS", Window.FPS);
        float remainWidth = ImGui.getContentRegionAvailX();
        float textWidth = ImGui.calcTextSizeX(fps);
        float offset = Math.max(remainWidth - textWidth, 0.0f);
        ImGui.setCursorPos(cursorPos.x + offset, cursorPos.y);
        ImGui.text(fps);
    }

    private void renderSceneName() {
        if (renaming) {
            ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
            ImGui.setKeyboardFocusHere();
            ImGui.inputTextWithHint("New name:##SEV_RenameScene", "Enter a new name or press Escape key to cancel...", renameBuffer);
            boolean focus = ImGui.isItemFocused();
            if (focus && ImGui.isKeyPressed(GLFW_KEY_ESCAPE)) {
                renaming = false;
                renameFocus = false;
                ImGui.popItemWidth();
                return;
            }
            if ((focus && ImGui.isKeyPressed(GLFW_KEY_ENTER)) || (renameFocus && !focus)) rename();
            renameFocus = focus;
            ImGui.popItemWidth();
            return;
        }
        boolean noScene = currentSceneName == null;
        if (noScene) ImGui.beginDisabled();
        EditorWidget.textCenterAlign(noScene ? "Empty" : currentSceneName);
        if (noScene) {
            ImGui.endDisabled();
            return;
        }
        if (!ImGui.isItemHovered()) return;
        if (LogicServer.currentSceneName() == null) {
            ImGui.setTooltip("Double click to save the scene");
            if (ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left)) SaveSceneAsDialog.show();
            return;
        }
        ImGui.setTooltip("Double click to rename the scene");
        if (!ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left)) return;
        renaming = true;
        renameFocus = false;
        renameBuffer.set(currentSceneName);
    }

    private void rename() {
        renaming = false;
        renameFocus = false;
        String newName = renameBuffer.get().trim();
        if (newName.isEmpty() || newName.equals(currentSceneName)) return;
        if (SceneManager.renameScene(currentSceneName, newName)) currentSceneName = newName;
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
        Viewport viewport = LogicServer.currentSceneViewport();
        if (!LogicServer.runtimeMode() && viewport != null) {
            viewport.adjustSceneScale(usableHeight);
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
        if (event instanceof RuntimeEvent runtimeEvent) {
            switch (runtimeEvent.type) {
                case RuntimeStopped, RuntimeCrashed -> isPlaying = false;
                case RuntimeStarted -> isPlaying = true;
            }
            renaming = false;
            renameFocus = false;
            return;
        }
        if (!(event instanceof SceneEvent sceneEvent)) return;
        SceneEvent.Type type = sceneEvent.type;
        if (type != SceneEvent.Type.SceneEntered && type != SceneEvent.Type.SceneLeaved) return;
        renaming = false;
        renameFocus = false;
    }

    private String resolveDisplaySceneName() {
        if (LogicServer.currentScene() == null) return null;
        String sceneName = LogicServer.currentSceneName();
        if (sceneName == null) return "Untitled";
        if (currentSceneName == null || !currentSceneName.equals(sceneName)) return sceneName;
        return currentSceneName;
    }
}

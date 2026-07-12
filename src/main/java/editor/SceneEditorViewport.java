package editor;

import TheCellBeyond.MouseListener;
import TheCellBeyond.Viewport;
import TheCellBeyond.Window;
import TheCellBeyond.internal.LogicServer;
import editor.components.EditorGizmoCtrl;
import editor.dialogs.SaveSceneAsDialog;
import editor.preference.UserPreference;
import eventviewer.EngineEventCallback;
import eventviewer.EngineEventListener;
import eventviewer.event.Event;
import eventviewer.event.RuntimeEvent;
import eventviewer.event.SceneEvent;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import org.joml.Vector2f;
import project.Project;
import project.ProjectPreference;
import project.WindowResizeMode;
import scene.SceneManager;

final class SceneEditorViewport implements EngineEventListener {
    private static final SceneEditorViewport instance = new SceneEditorViewport();
    static volatile String WindowID = "2D Scene###Editor_Scene_Viewport";
    private static float leftX, rightX, topY, bottomY;
    private static boolean isPlaying = false;
    private static String currentSceneName = null;
    private static boolean renaming = false;
    private static boolean renameFocus = false;
    private static final ImString renameBuffer = new ImString(128);
    private static float currentWidth;
    private static float currentHeight;
    private static final float IconSize = 28.0f;
    private static final ImVec2 gizmoModeSizeCache = new ImVec2(28.0f, 56.0f);

    private SceneEditorViewport() {
        register();
    }

    static void imgui() {
        currentSceneName = resolveDisplaySceneName();
        if (!ImGui.begin(WindowID, ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse | ImGuiWindowFlags.MenuBar | ImGuiWindowFlags.NoCollapse)) {
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
        ImBoolean snapGrid = new ImBoolean(UserPreference.preferences().showGridLine());
        if (ImGui.checkbox("Grid snapping##Ctrl_Grid_Snap_nd_Show_ESV", snapGrid)) {
            boolean enable = snapGrid.get();
            UserPreference.updatePreferences(UserPreference.preferences().showGridLine(enable));
        }
        ImGui.endTable();
        ImGui.endMenuBar();
        ImVec2 cursorPos = ImGui.getCursorPos();
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
        ImGui.image(texID, winSize.x, winSize.y, 0, 1, 1, 0);
        renderFPS(cursorPos);
        ImGui.endGroup();
        renderGizmoMode();
        MouseListener.setCurrentViewportPosition(new Vector2f(leftX, bottomY));
        MouseListener.setCurrentViewportSize(new Vector2f(winSize.x, winSize.y));
        ImGui.end();
    }

    private static void renderFPS(ImVec2 cursorPos) {
        String fps = String.format("%.2f FPS", Window.FPS);
        float remainWidth = ImGui.getContentRegionMaxX() - ImGui.getStyle().getWindowPaddingX();
        float textWidth = ImGui.calcTextSizeX(fps);
        float offset = Math.max(remainWidth - textWidth, 0.0f);
        ImGui.setCursorPos(cursorPos.x + offset, cursorPos.y);
        ImGui.text(fps);
    }

    private static void renderSceneName() {
        if (renaming) {
            ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
            ImGui.setKeyboardFocusHere();
            ImGui.inputTextWithHint("New name:##SEV_RenameScene", "Enter a new name or press Escape key to cancel...", renameBuffer);
            boolean focus = ImGui.isItemFocused();
            if (focus && ImGui.isKeyPressed(ImGuiKey.Escape)) {
                renaming = false;
                renameFocus = false;
                ImGui.popItemWidth();
                return;
            }
            if ((focus && ImGui.isKeyPressed(ImGuiKey.Enter)) || (renameFocus && !focus)) rename();
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

    private static void renderGizmoMode() {
        if (LogicServer.runtimeMode()) return;
        ImGui.setCursorPosY(ImGui.getContentRegionMaxY() - gizmoModeSizeCache.y * 2.0f - ImGui.getStyle().getWindowPaddingY());
        float cursorX = ImGui.getContentRegionMaxX() - gizmoModeSizeCache.x - ImGui.getStyle().getWindowPaddingX() ;
        ImGui.beginGroup();
        ImGui.setCursorPosX(cursorX);
        if (EditorWidget.selectableIcon("##SEV_Gizmo_Move_Mode_Selectable", EditorIcons.GizmoModeIcons.MoveMode, null, EditorGizmoCtrl.inMoveMode(), IconSize, IconSize)) EditorGizmoCtrl.useMoveGizmo();
        ImGui.getItemRectSize(gizmoModeSizeCache);
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.textColored(EditorColors.YellowHighLight, "Move Drag Handle");
            ImGui.sameLine();
            ImGui.textDisabled("Shift + T");
            ImGui.spacing();
            ImGui.textColored(EditorColors.InstructionHighLight, "When selecting an object, use the arrow handle to move it.");
            ImGui.endTooltip();
        }
        ImGui.setCursorPosX(cursorX);
        if (EditorWidget.selectableIcon("##SEV_Gizmo_Scale_Mode_Selectable", EditorIcons.GizmoModeIcons.ScaleMode, null, EditorGizmoCtrl.inScaleMode(), IconSize, IconSize)) EditorGizmoCtrl.useScaleGizmo();
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.textColored(EditorColors.YellowHighLight, "Scale Drag Handle");
            ImGui.sameLine();
            ImGui.textDisabled("Shift + S");
            ImGui.spacing();
            ImGui.textColored(EditorColors.InstructionHighLight, "When selecting an object, use the arrow handle to scale it.");
            ImGui.endTooltip();
        }
        ImGui.endGroup();
    }

    private static void rename() {
        renaming = false;
        renameFocus = false;
        String newName = renameBuffer.get().trim();
        if (newName.isEmpty() || newName.equals(currentSceneName)) return;
        if (SceneManager.renameScene(currentSceneName, newName)) currentSceneName = newName;
    }

    public static boolean getWantCaptureMouse() {
        if (ImGui.isPopupOpen("", ImGuiPopupFlags.AnyPopup)) return false;
        return MouseListener.getX() >= leftX &&
                MouseListener.getX() <= rightX &&
                MouseListener.getY() >= bottomY &&
                MouseListener.getY() <= topY;
    }

    /**
     * Calculate max viewport size base on available space and viewport's properties.
     * Size is clamped when the project does not allow window resize so it does not go above the game window's dimension preferences.
     * @return the usable space size vector in pixels
     */
    private static ImVec2 getMaxViewportSize() {
        ImVec2 avail = ImGui.getContentRegionAvail();
        Viewport viewport = LogicServer.currentSceneViewport();
        boolean runtime = LogicServer.runtimeMode();
        WindowResizeMode mode = runtime && viewport != null ? viewport.resizeMode() : WindowResizeMode.Expand;
        boolean maintainAspectRatio = viewport != null && viewport.maintainAspectRatio();
        ProjectPreference preference = Project.preference();
        int gameWidth = preference != null ? preference.gameWindowWidth() : 1;
        int gameHeight = preference != null ? preference.gameWindowHeight() : 1;
        boolean allowResize = preference == null || preference.allowResize();
        ImVec2 usableSpace = calculateUsableSize(avail, Project.getGameAspectRatio(), maintainAspectRatio, runtime, allowResize, gameWidth, gameHeight);
        if (usableSpace.x != currentWidth || usableSpace.y != currentHeight) {
            currentWidth = usableSpace.x;
            currentHeight = usableSpace.y;
        }
        if (viewport == null || mode != WindowResizeMode.Expand) return usableSpace;
        viewport.updateAspectRatio(usableSpace.x, usableSpace.y);
        viewport.adjustSceneScale(usableSpace.y);
        return usableSpace;
    }

    private static ImVec2 calculateUsableSize(ImVec2 availSpace, float gameAspectRatio, boolean maintainAspectRatio, boolean runtime, boolean allowResize, int gameWidth, int gameHeight) {
        float usableWidth = availSpace.x;
        float usableHeight = availSpace.y;
        if (maintainAspectRatio) {
            usableHeight = usableWidth / gameAspectRatio;
            if (usableHeight > availSpace.y) {
                usableHeight = availSpace.y;
                usableWidth = usableHeight * gameAspectRatio;
            }
        }
        if (!runtime || allowResize) return new ImVec2(usableWidth, usableHeight);
        usableWidth = Math.min(usableWidth, gameWidth);
        usableHeight = Math.min(usableHeight, gameHeight);
        return new ImVec2(usableWidth, usableHeight);
    }

    private static ImVec2 getViewportToCentral(ImVec2 usableSize) {
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

    private static String resolveDisplaySceneName() {
        if (LogicServer.currentScene() == null) return null;
        String sceneName = LogicServer.currentSceneName();
        if (sceneName == null) return "Untitled";
        if (currentSceneName == null || !currentSceneName.equals(sceneName)) return sceneName;
        return currentSceneName;
    }
}

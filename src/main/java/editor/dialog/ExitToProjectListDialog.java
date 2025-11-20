package editor.dialog;

import TheCellBeyond.Window;
import editor.ExitToProjectList;
import editor.preference.EditorPreferences;
import editor.preference.UserPreference;
import eventviewer.EngineEventCallback;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

public class ExitToProjectListDialog {
    private static final String POPUP_ID = "Save before exit?";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(400, 160);
    private static boolean showDialog = false;
    private static boolean isAutoSaveOnExit = false;
    private static final ImBoolean enableSaveOnExit = new ImBoolean(false);

    public static void show() {
        showDialog = true;
        isAutoSaveOnExit = UserPreference.reloadEditorPreferences().autoSaveOnExit();
    }

    public static void imgui() {
        if (!showDialog) return;

        if (isAutoSaveOnExit) {
            EngineEventCallback.emit(null, new Event(EventType.LEVEL_SAVE));
            Window.get().forceClose();
            showDialog = false;
            return;
        }

        ImGui.openPopup(POPUP_ID);

        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;

        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DIALOG_SIZE);

        if (ImGui.beginPopupModal(POPUP_ID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.spacing();
            ImGui.textWrapped("Save before exit? All unsaved changes will be lost.");
            ImGui.setCursorPosY(ImGui.getCursorPosY() + ImGui.getTextLineHeight());
            ImGui.separator();
            ImGui.spacing();
            ImGui.checkbox("Enable auto save on exit", enableSaveOnExit);

            float buttonWidth = 100;
            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - ImGui.getStyle().getWindowPaddingY());
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float saveX = (availX * 0.15f) - (buttonPivotX);
            float noSaveX = (availX * 0.5f) - (buttonPivotX);
            float cancelX = (availX * 0.85f) - (buttonPivotX);

            ImGui.setCursorPosX(saveX);
            if (ImGui.button("Save", buttonWidth, 0)) {
                if (enableSaveOnExit.get()) setAutoSaveOn();
                ExitToProjectList.get().toProjectList(true);
                EngineEventCallback.emit(null, new Event(EventType.LEVEL_SAVE));
                Window.get().forceClose();
                showDialog = false;
                ImGui.closeCurrentPopup();
            }

            ImGui.sameLine();
            ImGui.setCursorPosX(noSaveX);
            if (ImGui.button("Don't save", buttonWidth, 0)) {
                if (enableSaveOnExit.get()) setAutoSaveOn();
                ExitToProjectList.get().toProjectList(true);
                Window.get().forceClose();
                showDialog = false;
                ImGui.closeCurrentPopup();
            }

            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, 0)) {
                showDialog = false;
                ImGui.closeCurrentPopup();

            }

            ImGui.endPopup();
        }

        if (!ImGui.isPopupOpen(POPUP_ID)) showDialog = false;
    }

    private static void setAutoSaveOn() {
        EditorPreferences current = UserPreference.reloadEditorPreferences();

        if (current.autoSaveOnExit()) return;

        EditorPreferences update = new EditorPreferences(true, current.autoSaveOnChangeScene(), current.showGridLine());

        UserPreference.updateEditorPreferences(update);
    }
}

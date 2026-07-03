package editor.dialog;

import TheCellBeyond.Window;
import TheCellBeyond.internal.LogicServer;
import editor.ExitToProjectList;
import editor.preference.EditorPreferences;
import editor.preference.UserPreference;
import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

/**
 * Editor dialogue for saving before exiting back to project list.
 */
public final class ExitToProjectListDialog {
    private static final String PopupID = "Save before exit?";
    private static final ImVec2 DialogSize = new ImVec2(400, 160);
    private static boolean showDialog = false;
    private static boolean isAutoSaveOnExit = false;
    private static final ImBoolean enableSaveOnExit = new ImBoolean(false);

    private ExitToProjectListDialog() {}

    /**
     * Toggle the show flag for this dialogue.
     */
    public static void show() {
        showDialog = true;
        isAutoSaveOnExit = UserPreference.reloadPreferences().autoSaveOnExit();
    }

    /**
     * Render the dialogue on scene, this will be skip if auto save on exit is enabled.
     */
    public static void imgui() {
        if (!showDialog) return;
        if (isAutoSaveOnExit) {
            if (LogicServer.currentSceneName() == null) SaveSceneAsDialog.show(ExitToProjectListDialog::saveAndExit);
            else saveAndExit();
            showDialog = false;
            return;
        }
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.spacing();
            ImGui.textWrapped("Save before exit? All unsaved changes will be lost.");
            ImGui.setCursorPosY(ImGui.getCursorPosY() + ImGui.getTextLineHeight());
            ImGui.separator();
            ImGui.spacing();
            ImGui.checkbox("Enable auto save on exit", enableSaveOnExit);
            float buttonWidth = 100;
            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - ImGui.getStyle().getWindowPaddingY());
            float startX = ImGui.getCursorStartPosX();
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float saveX = startX + availX * 0.15f - buttonPivotX;
            float noSaveX = startX + availX * 0.5f - buttonPivotX;
            float cancelX = startX + availX * 0.85f - buttonPivotX;
            ImGui.setCursorPosX(saveX);
            if (ImGui.button("Save", buttonWidth, 0)) {
                if (enableSaveOnExit.get()) enableAutosave();
                if (LogicServer.currentSceneName() == null) SaveSceneAsDialog.show(ExitToProjectListDialog::saveAndExit);
                else saveAndExit();
                showDialog = false;
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            ImGui.setCursorPosX(noSaveX);
            if (ImGui.button("Don't save", buttonWidth, 0)) {
                if (enableSaveOnExit.get()) enableAutosave();
                ExitToProjectList.toProjectList(true);
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
        if (!ImGui.isPopupOpen(PopupID)) showDialog = false;
    }

    private static void enableAutosave() {
        EditorPreferences current = UserPreference.reloadPreferences();
        if (current.autoSaveOnExit()) return;
        UserPreference.updatePreferences(current.autoSaveOnExit(true));
    }

    private static void saveAndExit() {
        ExitToProjectList.toProjectList(true);
        EngineEventCallback.emit(new EditorEvent(EditorEvent.Type.SaveEditingSceneToDisk));
        Window.get().forceClose();
    }
}

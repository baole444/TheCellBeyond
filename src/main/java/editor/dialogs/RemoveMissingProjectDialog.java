package editor.dialogs;

import editor.preference.RecentProject;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;

public final class RemoveMissingProjectDialog {
    private static final String PopupID = "Missing project";
    private static final ImVec2 DialogSize = new ImVec2(400.0f, 160.0f);
    private static boolean showDialog = false;
    private static Runnable onRemoveCallback = null;
    private static RecentProject selectedProject = null;

    public static void show(Runnable onRemove, RecentProject recentProject) {
        showDialog = true;
        onRemoveCallback = onRemove;
        selectedProject = recentProject;
    }

    public static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.spacing();
            String title = "NULL";
            if (selectedProject != null) title = selectedProject.title() != null ? selectedProject.title() : "Unknow title";
            ImGui.textWrapped("Selected project '" + title + "'cannot be found. Remove it from the list?");
            ImGui.spacing();
            float buttonWidth = 100.0f;
            float buttonHeight = 30.0f;
            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - buttonHeight / 2.0f);
            float startX = ImGui.getCursorStartPosX();
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float removeX = startX + availX * 0.25f - buttonPivotX;
            float cancelX = startX + availX * 0.75f - buttonPivotX;
            ImGui.setCursorPosX(removeX);
            if (ImGui.button("Remove", buttonWidth, buttonHeight)) {
                if (onRemoveCallback != null) {
                    Runnable remove = onRemoveCallback;
                    remove.run();
                }
                onRemoveCallback = null;
                showDialog = false;
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, buttonHeight)) {
                showDialog = false;
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }
        if (!ImGui.isPopupOpen(PopupID)) {
            showDialog = false;
            onRemoveCallback = null;
        }
    }
}
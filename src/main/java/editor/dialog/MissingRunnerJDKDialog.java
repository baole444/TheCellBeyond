package editor.dialog;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;

public final class MissingRunnerJDKDialog {
    private static final String PopupID = "Missing Gradle JVM##TCB_Missing_Gradle_JVM_Modal";
    private static final ImVec2 DialogSize = new ImVec2(400.0f, 200.0f);
    private static final float ButtonWidth = 100.0f;
    private static final float ButtonHeight = 30;
    private static final float ButtonPivotX = ButtonWidth * 0.5f;
    private static boolean showDialog = false;

    private MissingRunnerJDKDialog() {}

    public static void show() {
        showDialog = true;
    }

    public static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 center = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(center.x, center.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.spacing();
            ImGui.textWrapped("No valid JDK selected to build your script. Would you like to configure it?");
            ImGui.spacing();
            float availX = ImGui.getContentRegionAvailX();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - ButtonHeight - ImGui.getStyle().getWindowPaddingY());
            ImGui.setCursorPosX((availX * 0.3f) - ButtonPivotX);
            if (ImGui.button("Yes", ButtonWidth, ButtonHeight)) {
                close();
                EditEditorPreferencesDialog.show();
            }
            ImGui.sameLine();
            ImGui.setCursorPosX((availX * 0.7f) - ButtonPivotX);
            if (ImGui.button("No", ButtonWidth, ButtonHeight)) close();
            ImGui.endPopup();
        }
        if (!ImGui.isPopupOpen(PopupID)) showDialog = false;
    }

    private static void close() {
        showDialog = false;
        ImGui.closeCurrentPopup();
    }
}

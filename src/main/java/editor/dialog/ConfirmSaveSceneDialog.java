package editor.dialog;

import TheCellBeyond.Window;
import eventviewer.EventSystem;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;

public class ConfirmSaveSceneDialog {
    private static final String POPUP_ID = "Save current scene?";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(400, 200);
    private static boolean showDialog = false;
    private static Runnable onCompleteDecision = null;

    public static void show(Runnable decisionCallback) {
        showDialog =  true;
        onCompleteDecision = decisionCallback;
    }

    public static void imgui() {
        if (!showDialog) return;

        ImGui.openPopup(POPUP_ID);

        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;

        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DIALOG_SIZE, ImGuiCond.FirstUseEver);

        if (ImGui.beginPopupModal(POPUP_ID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.textWrapped("Save before change scene? All unsaved changes will be lost.");
            ImGui.spacing();
            ImGui.separator();

            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - ImGui.getStyle().getWindowPaddingY());

            float buttonWidth = 100;
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float saveX = (availX * 0.15f) - (buttonPivotX);
            float noSaveX = (availX * 0.5f) - (buttonPivotX);
            float cancelX = (availX * 0.85f) - (buttonPivotX);

            ImGui.setCursorPosX(saveX);
            if (ImGui.button("Save", buttonWidth, 0)) {
                EventSystem.emit(null, new Event(EventType.LEVEL_SAVE));
                closeConfirmation();
            }

            ImGui.sameLine();
            ImGui.setCursorPosX(noSaveX);
            if (ImGui.button("Don't save")) closeConfirmation();

            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, 0)) {
                showDialog = false;
                onCompleteDecision = null;
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }

        if (!ImGui.isPopupOpen(POPUP_ID)) {
            showDialog = false;
            onCompleteDecision = null;
            ImGui.closeCurrentPopup();
        }
    }

    private static void closeConfirmation() {
        showDialog = false;
        ImGui.closeCurrentPopup();

        if (onCompleteDecision == null) return;

        Runnable callback = onCompleteDecision;
        onCompleteDecision = null;
        callback.run();
    }
}

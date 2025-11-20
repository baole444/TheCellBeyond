package editor.dialog;

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

public class ConfirmSaveSceneDialog {
    private static final String POPUP_ID = "Save current scene?";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(400, 160);
    private static boolean showDialog = false;
    private static Runnable onCompleteDecision = null;
    private static Runnable onCancelDecision = null;
    private static boolean isAutoSaveOnSceneChange = false;
    private static final ImBoolean enableSaveOnChangeScene = new ImBoolean(false);

    public static void show(Runnable decisionCallback) {
        show(decisionCallback, null);
    }

    public static void show(Runnable decisionCallback, Runnable cancelCallback) {
        isAutoSaveOnSceneChange = UserPreference.editorPreferences().autoSaveOnChangeScene();
        showDialog = true;
        onCompleteDecision = decisionCallback;
        onCancelDecision = cancelCallback;
    }

    public static void imgui() {
        if (!showDialog) return;

        if (isAutoSaveOnSceneChange) {
            EngineEventCallback.emit(null, new Event(EventType.LEVEL_SAVE));
            closeConfirmation();
            return;
        }

        ImGui.openPopup(POPUP_ID);

        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;

        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DIALOG_SIZE);

        if (ImGui.beginPopupModal(POPUP_ID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.textWrapped("Save before change scene? All unsaved changes will be lost.");
            ImGui.setCursorPosY(ImGui.getCursorPosY() + ImGui.getTextLineHeight());
            ImGui.separator();
            ImGui.spacing();
            ImGui.checkbox("Enable auto save on change scene", enableSaveOnChangeScene);

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
                if (enableSaveOnChangeScene.get()) setAutoSaveOn();
                EngineEventCallback.emit(null, new Event(EventType.LEVEL_SAVE));
                closeConfirmation();
            }

            ImGui.sameLine();
            ImGui.setCursorPosX(noSaveX);
            if (ImGui.button("Don't save")) {
                if (enableSaveOnChangeScene.get()) setAutoSaveOn();
                closeConfirmation();
            }

            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, 0)) {
                showDialog = false;
                onCompleteDecision = null;
                Runnable cancelCallback = onCancelDecision;
                onCancelDecision = null;
                ImGui.closeCurrentPopup();

                if (cancelCallback != null)  cancelCallback.run();
            }

            ImGui.endPopup();
        }

        if (!ImGui.isPopupOpen(POPUP_ID)) {
            showDialog = false;
            onCompleteDecision = null;
            onCancelDecision = null;
        }
    }

    private static void closeConfirmation() {
        showDialog = false;
        ImGui.closeCurrentPopup();

        if (onCompleteDecision == null) return;

        Runnable callback = onCompleteDecision;
        onCompleteDecision = null;
        onCancelDecision = null;
        callback.run();
    }

    private static void setAutoSaveOn() {
        EditorPreferences current = UserPreference.reloadEditorPreferences();

        if (current.autoSaveOnChangeScene()) return;

        EditorPreferences update = new EditorPreferences(current.autoSaveOnExit(), true, current.showGridLine());

        UserPreference.updateEditorPreferences(update);
    }
}

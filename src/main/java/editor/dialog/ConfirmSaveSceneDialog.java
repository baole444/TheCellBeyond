package editor.dialog;

import TheCellBeyond.internal.LogicServer;
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
 * Editor dialogue for confirm saving current editing scene.
 */
public class ConfirmSaveSceneDialog {
    private static final String PopupID = "Save current scene?";
    private static final ImVec2 DialogSize = new ImVec2(400, 160);
    private static boolean showDialog = false;
    private static Runnable onCompleteDecision = null;
    private static Runnable onCancelDecision = null;
    private static boolean isAutoSaveOnSceneChange = false;
    private static final ImBoolean enableSaveOnChangeScene = new ImBoolean(false);

    /**
     * Create the dialogue module.
     */
    private ConfirmSaveSceneDialog() {}

    /**
     * Toggle the show flag of this dialogue.
     * @param decisionCallback callback to execute on decision accept
     */
    public static void show(Runnable decisionCallback) {
        show(decisionCallback, null);
    }

    /**
     * Toggle the show flag of this dialogue.
     * @param decisionCallback callback to execute on decision accept
     * @param cancelCallback callback to execute on decision reject
     */
    public static void show(Runnable decisionCallback, Runnable cancelCallback) {
        isAutoSaveOnSceneChange = UserPreference.editorPreferences().autoSaveOnChangeScene();
        showDialog = true;
        onCompleteDecision = decisionCallback;
        onCancelDecision = cancelCallback;
    }

    /**
     * Render this dialogue on screen.
     */
    public static void imgui() {
        if (!showDialog) return;
        if (LogicServer.currentScene() == null) {
            closeConfirmation();
            return;
        }
        if (isAutoSaveOnSceneChange && LogicServer.currentSceneName() != null) {
            EngineEventCallback.emit(null, new EditorEvent(EditorEvent.Type.SaveEditingSceneToDisk));
            closeConfirmation();
            return;
        }
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
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
            if (ImGui.button("Save", buttonWidth, 0.0f)) {
                if (enableSaveOnChangeScene.get()) setAutoSaveOn();
                if (LogicServer.currentSceneName() == null) {
                    Runnable nextStep = onCompleteDecision;
                    showDialog = false;
                    onCompleteDecision = null;
                    onCancelDecision = null;
                    ImGui.closeCurrentPopup();
                    SaveSceneAsDialog.show(nextStep);
                }
                else {
                    EngineEventCallback.emit(null, new EditorEvent(EditorEvent.Type.SaveEditingSceneToDisk));
                    closeConfirmation();
                }
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
        if (!ImGui.isPopupOpen(PopupID)) {
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

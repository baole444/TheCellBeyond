package editor.dialog;

import editor.preference.EditorPreferences;
import editor.preference.UserPreference;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

/**
 * Editor dialogue for editing the editor behaviour and preferences.
 */
public final class EditEditorPreferencesDialog {
    private static final String PopupID = "Editor Preferences";
    private static final ImVec2 DialogSize = new ImVec2(720.0f, 400.0f);
    private static final float ButtonReserve = ImGui.getFrameHeightWithSpacing();
    private static final float Padding = 4.0f;
    private static boolean showDialog = false;
    private static final ImBoolean autoSaveOnExit = new ImBoolean(false);
    private static final ImBoolean autoSaveOnChangeScene = new ImBoolean(false);
    private static final ImBoolean showGridLine = new ImBoolean(false);
    private static final ImBoolean cleanBuildScripts = new ImBoolean(true);
    private static boolean editorPreferenceChanged = false;

    private EditEditorPreferencesDialog() {}

    /**
     * Toggle the show flag for this dialogue.
     */
    public static void show() {
        showDialog = true;
        syncWithEditor();
    }

    /**
     * Render the dialogue on screen.
     */
    public static void imgui() {
        if (!showDialog) return;
        syncWithEditor();
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.text("Adjust Editor's preferences");
            float buttonWidth = 120;
            float buttonHeight = 30;
            float regionHeight = ImGui.getContentRegionAvailY() - ButtonReserve - buttonHeight - Padding;
            if (ImGui.beginChild("##EEPD_Preference_Region", 0.0f, regionHeight, ImGuiChildFlags.Borders)) renderPreferenceEditor();
            ImGui.endChild();
            autoSavePreference();
            renderCloseButton(buttonWidth, buttonHeight);
            ImGui.endPopup();
        }
        if (!ImGui.isPopupOpen(PopupID)) showDialog = false;
    }

    private static void renderCloseButton(float buttonWidth, float buttonHeight) {
        ImGui.setCursorPosY(ImGui.getWindowHeight() - ButtonReserve - (buttonHeight / 2) - ImGui.getStyle().getWindowPaddingY());
        float buttonPivotX = buttonWidth * 0.5f;
        float availX = ImGui.getContentRegionAvailX();
        float closeX = (availX * 0.5f) - buttonPivotX;
        ImGui.setCursorPosX(closeX);
        if (!ImGui.button("Close##EPPD_CLose_Dialog", buttonWidth, buttonHeight)) return;
        showDialog = false;
        ImGui.closeCurrentPopup();
    }

    private static void renderPreferenceEditor() {
        ImGui.text("Auto save:");
        ImGui.spacing();
        renderPreferenceToggle("On exit", autoSaveOnExit, " - Auto save current scene on exiting editor");
        ImGui.spacing();
        renderPreferenceToggle("On change scene", autoSaveOnChangeScene, " - Auto save current scene on switching to a new one");
        ImGui.spacing();
        ImGui.separator();
        ImGui.text("Object Control:");
        ImGui.spacing();
        renderPreferenceToggle("Grid snapping", showGridLine, " - Holding object snap to nearest grid square");
        ImGui.spacing();
        ImGui.separator();
        ImGui.text("Scripting:");
        ImGui.spacing();
        renderPreferenceToggle("Clean before build", cleanBuildScripts, " - Clear old build's output before new build start");
        ImGui.spacing();
    }

    private static void renderPreferenceToggle(String label, ImBoolean dest, String extraInfo) {
        if (ImGui.checkbox(label + "##EEPD_" + label, dest)) editorPreferenceChanged = true;
        ImGui.sameLine();
        ImGui.beginDisabled();
        ImGui.textWrapped(extraInfo);
        ImGui.endDisabled();
    }

    private static void syncWithEditor() {
        editorPreferenceChanged = false;
        EditorPreferences preferences = UserPreference.preferences();
        autoSaveOnChangeScene.set(preferences.autoSaveOnChangeScene());
        autoSaveOnExit.set(preferences.autoSaveOnExit());
        showGridLine.set(preferences.showGridLine());
        cleanBuildScripts.set(preferences.cleanBuildScripts());
    }

    private static void autoSavePreference() {
        if (!editorPreferenceChanged) return;
        UserPreference.updatePreferences(new EditorPreferences(
                autoSaveOnExit.get(),
                autoSaveOnChangeScene.get(),
                showGridLine.get(),
                cleanBuildScripts.get()
        ));
        editorPreferenceChanged = false;
    }
}

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
    private static final String PreferenceID = "Preference_Editor";
    private static final ImVec2 DialogSize = new ImVec2(720.0f, 400.0f);
    private static boolean showDialog = false;
    private static final ImBoolean autoSaveOnExit = new ImBoolean(false);
    private static final ImBoolean autoSaveOnChangeScene = new ImBoolean(false);
    private static final float metaYPercentage = 0.85f;

    /**
     * Create the dialogue module.
     */
    private EditEditorPreferencesDialog() {}

    /**
     * Toggle the show flag for this dialogue.
     */
    public static void show() {
        showDialog = true;
        loadFromPreference();
    }

    /**
     * Render the dialogue on screen.
     */
    public static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.text("Adjust editor's references");
            renderPreferenceEditor();
            float buttonWidth = 120;
            float buttonHeight = 30;
            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - (buttonHeight / 2) - ImGui.getStyle().getWindowPaddingY());
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float applyX = (availX * 0.15f) - buttonPivotX;
            float saveX = (availX * 0.5f) - buttonPivotX;
            float cancelX = (availX * 0.85f) - buttonPivotX;
            ImGui.setCursorPosX(applyX);
            if (ImGui.button("Apply", buttonWidth, buttonHeight)) savePreference();
            ImGui.sameLine();
            ImGui.setCursorPosX(saveX);
            if (ImGui.button("Save", buttonWidth, buttonHeight)) {
                savePreference();
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
        if (!ImGui.isPopupOpen(PopupID)) showDialog = false;
    }

    private static void renderPreferenceEditor() {
        int sectionY = (int) (ImGui.getContentRegionAvailY() * metaYPercentage);
        ImGui.beginChild(PreferenceID, new ImVec2(0.0f, sectionY), ImGuiChildFlags.Border);
        ImGui.text("Auto save:");
        ImGui.spacing();
        ImGui.checkbox("On exit", autoSaveOnExit);
        ImGui.sameLine();
        ImGui.beginDisabled();
        ImGui.textWrapped("(auto save scene on exiting editor)");
        ImGui.endDisabled();
        ImGui.spacing();
        ImGui.checkbox("On change scene", autoSaveOnChangeScene);
        ImGui.sameLine();
        ImGui.beginDisabled();
        ImGui.textWrapped("(auto save scene on switching to different scene)");
        ImGui.endDisabled();
        ImGui.spacing();
        ImGui.separator();
        ImGui.endChild();
    }

    private static void loadFromPreference() {
        EditorPreferences preferences = UserPreference.editorPreferences();
        autoSaveOnExit.set(preferences.autoSaveOnExit());
        autoSaveOnChangeScene.set(preferences.autoSaveOnChangeScene());
    }

    private static void savePreference() {
        EditorPreferences newPref = new EditorPreferences(autoSaveOnExit.get(), autoSaveOnChangeScene.get(), UserPreference.editorPreferences().showGridLine());
        UserPreference.updateEditorPreferences(newPref);
    }
}

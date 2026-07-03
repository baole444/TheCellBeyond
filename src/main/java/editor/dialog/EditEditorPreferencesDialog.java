package editor.dialog;

import editor.EditorIcons;
import editor.EditorWidget;
import editor.preference.EditorPreferences;
import editor.preference.JDKRegistry;
import editor.preference.UserPreference;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImBoolean;
import scripting.builder.jdk.JDKInstallation;
import scripting.builder.jdk.JDKManager;

import java.nio.file.Path;
import java.util.List;

/**
 * Editor dialogue for editing the editor behaviour and preferences.
 */
public final class EditEditorPreferencesDialog {
    private static final String PopupID = "Editor Preferences";
    private static final ImVec2 DialogSize = new ImVec2(720.0f, 400.0f);
    private static final float ButtonReserve = ImGui.getFrameHeightWithSpacing();
    private static final float ButtonWidth = 120.0f;
    private static final float ButtonHeight = 30.0f;
    private static boolean showDialog = false;
    private static final ImBoolean autoSaveOnExit = new ImBoolean(false);
    private static final ImBoolean autoSaveOnChangeScene = new ImBoolean(false);
    private static final ImBoolean showGridLine = new ImBoolean(false);
    private static final ImBoolean cleanBuildScripts = new ImBoolean(true);
    private static final ImVec2 tmpCursorPos = new ImVec2();
    private static final ImBoolean tmpInteracted = new ImBoolean();
    private static JDKInstallation selectedJDK = null;
    private static boolean editorPreferenceChanged = false;
    private static boolean showDownloadJDKDialog = false;

    private EditEditorPreferencesDialog() {}

    /**
     * Toggle the show flag for this dialogue.
     */
    public static void show() {
        showDialog = true;
        syncWithEditor();
    }

    static void closeDownloadJDKDialog() {
        showDownloadJDKDialog = false;
    }

    /**
     * Render the dialogue on screen.
     */
    public static void imgui() {
        if (!showDialog) return;
        syncWithEditor();
        if (showDownloadJDKDialog) {
            DownloadJDKDialog.imgui();
            return;
        }
        ImGui.openPopup(PopupID);
        ImVec2 center = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(center.x, center.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.text("Adjust Editor's preferences");
            float regionHeight = ImGui.getContentRegionAvailY() - ButtonReserve - ButtonHeight;
            if (ImGui.beginChild("##EEPD_Preference_Region", 0.0f, regionHeight, ImGuiChildFlags.Borders)) renderPreferenceEditor();
            ImGui.endChild();
            autoSavePreference();
            renderCloseButton();
            ImGui.endPopup();
        }
        if (!ImGui.isPopupOpen(PopupID)) showDialog = false;
    }

    private static void renderCloseButton() {
        ImGui.setCursorPosY(ImGui.getWindowHeight() - ButtonReserve - ButtonHeight / 2.0f);
        float buttonPivotX = ButtonWidth * 0.5f;
        float availX = ImGui.getContentRegionAvailX();
        float closeX = ImGui.getCursorStartPosX() + availX * 0.5f - buttonPivotX;
        ImGui.setCursorPosX(closeX);
        if (!ImGui.button("Close##EPPD_CLose_Dialog", ButtonWidth, ButtonHeight)) return;
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
        renderGradleJVM();
    }

    private static void renderPreferenceToggle(String label, ImBoolean dest, String extraInfo) {
        if (ImGui.checkbox(label + "##EEPD_" + label, dest)) editorPreferenceChanged = true;
        ImGui.sameLine();
        ImGui.beginDisabled();
        ImGui.textWrapped(extraInfo);
        ImGui.endDisabled();
    }

    private static void renderGradleJVM() {
        if (!ImGui.beginTable("##EEPD_Gradle_JVM_Layout_Table", 2)) return;
        ImGui.tableSetupColumn("##EEPD_Gradle_JVM_Label_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##EEPD_Gradle_JVM_Drop_Down_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableNextColumn();
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (ImGui.getFrameHeightWithSpacing() - ImGui.getTextLineHeightWithSpacing()) / 2.0f);
        ImGui.text("Gradle JVM:");
        ImGui.tableNextColumn();
        renderJDKDropDown();
        ImGui.sameLine();
        if (EditorWidget.iconButton("Rescan##EEPD_Rescan_JDK_Button", EditorIcons.Icons.Reset, "Scan for JDK installations again")) UserPreference.rescanJDKs();
        ImGui.endTable();
    }

    private static void renderJDKDropDown() {
        JDKInstallation javaHome = JDKManager.javaHome();
        List<JDKInstallation> userAdded = JDKManager.userAdded();
        List<JDKInstallation> detected = JDKManager.detected();
        int supID = 0;
        if (!ImGui.beginCombo("##EEPD_JDK_Selection_Combo", selectedJDK == null ? "Select JDK..." : selectedJDK.displayName(), ImGuiComboFlags.HeightLarge)) return;
        if (javaHome != null) renderJDKSelectable(javaHome, supID++);
        else ImGui.textDisabled("No JAVA_HOME detected");
        ImGui.separator();
        if (userAdded.isEmpty()) ImGui.textDisabled("No JDK added");
        else for (JDKInstallation jdk : userAdded) renderJDKSelectable(jdk, supID++);
        ImGui.separator();
        ImGui.pushStyleColor(ImGuiCol.Button, 0.0f, 0.0f, 0.0f, 0.0f);
        if (ImGui.selectable("Download JDK...##EEPD_Download_JDK_Button")) {
            showDownloadJDKDialog = true;
            DownloadJDKDialog.show();
        }
        if (ImGui.selectable("Add JDK from disk...##EEPD_Add_JDK_From_Disk_Button")) browseJDK();
        ImGui.popStyleColor(1);
        ImGui.separator();
        ImGui.text("Detected JDKs");
        ImGui.spacing();
        if (detected.isEmpty()) ImGui.textDisabled("No JDK detected");
        else for (JDKInstallation jdk : detected) renderJDKSelectable(jdk, supID++);
        ImGui.endCombo();
    }

    private static void renderJDKSelectable(JDKInstallation jdk, int supID) {
        if (jdk == null) return;
        boolean invalid = !jdk.valid();
        boolean currentlySelected = jdk == selectedJDK;
        ImGui.beginGroup();
        if (invalid) ImGui.beginDisabled();
        tmpCursorPos.set(ImGui.getCursorPos());
        tmpInteracted.set(ImGui.selectable("##EEP_Select_JDK_" + jdk.home() + "_" + supID, currentlySelected));
        ImGui.setItemTooltip(jdk.home().toString());
        ImGui.setCursorPos(tmpCursorPos);
        renderJDKLabel(jdk);
        if (invalid) ImGui.endDisabled();
        ImGui.endGroup();
        if (invalid || !tmpInteracted.get() || currentlySelected) return;
        selectedJDK = jdk;
        if (jdk.fromJavaHome()) UserPreference.selectJDK(JDKRegistry.JavaHomeSelection);
        else UserPreference.selectJDK(jdk.home());
    }

    private static void browseJDK() {
        Path selectedDir = OpenDirectoryDialog.openDialog();
        if (selectedDir == null) return;
        UserPreference.addJDK(selectedDir);
    }

    private static void renderJDKLabel(JDKInstallation jdk) {
        if (jdk == null) return;
        ImGui.text(jdk.displayName());
        String version = jdk.version();
        if (version == null || version.isBlank()) return;
        ImGui.sameLine();
        ImGui.textDisabled("- " + version);
    }

    private static void syncWithEditor() {
        editorPreferenceChanged = false;
        EditorPreferences preferences = UserPreference.preferences();
        autoSaveOnChangeScene.set(preferences.autoSaveOnChangeScene());
        autoSaveOnExit.set(preferences.autoSaveOnExit());
        showGridLine.set(preferences.showGridLine());
        cleanBuildScripts.set(preferences.cleanBuildScripts());
        selectedJDK = UserPreference.selectedJDK();
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

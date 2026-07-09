package editor.dialogs;

import editor.EditorIcons;
import editor.EditorWidget;
import editor.preference.UserPreference;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImString;
import scripting.builder.jdk.download.DownloadProgress;
import scripting.builder.jdk.download.JDKDownloader;
import scripting.builder.jdk.download.JDKProvider;

import java.nio.file.Path;

final class DownloadJDKDialog {
    private enum FeatureVersion {
        v25("25", 25),
        v26("26", 26);

        final String display;
        final int version;

        FeatureVersion(String display, int version) {
            this.display = display;
            this.version = version;
        }
    }

    private static final String PopupID = "Download JDK";
    private static final ImVec2 DialogSize = new ImVec2(480.0f, 240.0f);
    private static final float ButtonReserve = ImGui.getFrameHeightWithSpacing();
    private static final float ButtonWidth = 100.0f;
    private static final float ButtonHeight = 30.0f;
    private static boolean showDialog = false;
    private static final ImString selectedPathDisplay = new ImString(256);
    private static JDKProvider selectedProvider = JDKProvider.Temurin;
    private static FeatureVersion selectedVersion = FeatureVersion.v25;
    private static Path selectedPath = UserPreference.jdkInstallDir();
    private static volatile boolean downloadRequested = false;
    private static String message = "";

    static void show() {
        showDialog = true;
        message = "";
        selectedProvider = JDKProvider.Temurin;
        selectedVersion = FeatureVersion.v25;
        selectedPath = UserPreference.jdkInstallDir();
    }

    static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 center = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(center.x, center.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            refreshStatus();
            renderOptions();
            ImGui.spacing();
            EditorWidget.textCenterAlign(message);
            renderControlButtons();
            ImGui.endPopup();
        }
        if (!ImGui.isPopupOpen(PopupID)) showDialog = false;
    }

    private static void renderOptions() {
        ImGui.spacing();
        float margin = (ImGui.getContentRegionAvailX() * 0.15f) / 2.0f;
        ImGui.indent(margin);
        if (!ImGui.beginTable("##DJ_Selection_Layout_Table", 2, ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX() - margin, 0.0f)) {
            ImGui.unindent(margin);
            return;
        }
        ImGui.tableSetupColumn("##DJ_Selection_Label_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##DJ_Selection_Dropdown_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableNextColumn();
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (ImGui.getFrameHeightWithSpacing() - ImGui.getTextLineHeightWithSpacing()) / 2.0f);
        ImGui.textUnformatted("Version: ");
        ImGui.tableNextColumn();
        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
        if (ImGui.beginCombo("##DJ_Version_Selection_Combo", selectedVersion != null ? selectedVersion.display : "Select version...")) {
            for (FeatureVersion version : FeatureVersion.values()) {
                if (ImGui.selectable(version.display + "##DJ_Version_Selectable_" + version.display, version == selectedVersion)) selectedVersion = version;
            }
            ImGui.endCombo();
        }
        ImGui.tableNextColumn();
        ImGui.spacing();
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (ImGui.getFrameHeightWithSpacing() - ImGui.getTextLineHeightWithSpacing()) / 2.0f);
        ImGui.textUnformatted("Vendor:  ");
        ImGui.tableNextColumn();
        ImGui.spacing();
        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
        if (ImGui.beginCombo("##DJ_Vendor_Selection_Combo", selectedProvider != null ? selectedProvider.formalName : "Select vendor...")) {
            for (JDKProvider provider : JDKProvider.values()) {
                if (ImGui.selectable(provider.formalName + "##DJ_Version_Selectable_" + provider.distribution, provider == selectedProvider)) selectedProvider = provider;
            }
            ImGui.spacing();
            ImGui.beginDisabled();
            ImGui.textWrapped("More vendors will be added in the future");
            ImGui.endDisabled();
            ImGui.endCombo();
        }
        ImGui.endTable();
        if (!ImGui.beginTable("##DJ_Select_Location_Layout_Table", 3, ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX() - margin, 0.0f)) {
            ImGui.unindent(margin);
            return;
        }
        ImGui.tableSetupColumn("##DJ_Select_Location_Label_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##DJ_Select_Location_Input_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("##DJ_Select_Location_Browse_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableNextColumn();
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (ImGui.getFrameHeightWithSpacing() - ImGui.getTextLineHeightWithSpacing()) / 2.0f);
        ImGui.text("Location:");
        ImGui.tableNextColumn();
        if (selectedPath != null) selectedPathDisplay.set(selectedPath.toString());
        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
        ImGui.inputTextWithHint("##DJ_Select_Location_Input", "Click \"Select Directory\" to choose root directory...", selectedPathDisplay, ImGuiInputTextFlags.ReadOnly);
        if (ImGui.calcTextSizeX(selectedPathDisplay.get()) > ImGui.getContentRegionAvailX()) ImGui.setItemTooltip(selectedPathDisplay.get());
        ImGui.beginDisabled();
        ImGui.textWrapped("The JDK home directory will be created under the selected location");
        ImGui.endDisabled();
        ImGui.tableNextColumn();
        if (EditorWidget.iconButton("Select Directory##DJ_Select_Directory_Button", EditorIcons.Icons.Open, "Click to select the root directory to install the JDK into")) {
            Path selected = OpenDirectoryDialog.openDialog();
            if (selected != null) selectedPath = selected;
        }
        ImGui.endTable();
        ImGui.unindent(margin);
    }

    private static void renderControlButtons() {
        ImGui.setCursorPosY(ImGui.getWindowHeight() - ButtonReserve - ButtonHeight / 2.0f);
        float startX = ImGui.getCursorStartPosX();
        float buttonPivotX = ButtonWidth * 0.5f;
        float availX = ImGui.getContentRegionAvailX();
        float downloadX = startX + availX * 0.25f - buttonPivotX;
        float cancelX = startX + availX * 0.75f - buttonPivotX;
        ImGui.setCursorPosX(downloadX);
        boolean canDownload = selectedProvider != null && selectedVersion != null;
        if (!canDownload) ImGui.beginDisabled();
        if (ImGui.button("Download##DJ_Download_Selected_JDK_Button", ButtonWidth, ButtonHeight)) startDownload();
        if (!canDownload) ImGui.endDisabled();
        ImGui.sameLine();
        ImGui.setCursorPosX(cancelX);
        if (!ImGui.button("Cancel##DJ_Cancel_Button", ButtonWidth, ButtonHeight)) return;
        showDialog = false;
        ImGui.closeCurrentPopup();
        EditEditorPreferencesDialog.closeDownloadJDKDialog();
    }

    private static void startDownload() {
        if (!downloadRequested) return;
        downloadRequested = true;
        message = "Starting download...";
        UserPreference.downloadJDK(selectedPath ,selectedVersion.version, selectedProvider).whenComplete((_, _) -> downloadRequested = false);
    }

    private static void refreshStatus() {
        if (!downloadRequested) return;
        DownloadProgress snapshot = JDKDownloader.progress();
        if (snapshot == null) return;
        if (snapshot.phase() == DownloadProgress.Phase.Downloading) {
            message = snapshot.byteTotal() > 0 ? String.format("Downloading... %d%%", Math.round(snapshot.fraction() * 100.0f)) : "Downloading...";
            return;
        }
        message = snapshot.message();
    }
}

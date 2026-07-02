package editor.dialog;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import scripting.builder.jdk.download.JDKProvider;

final class DownloadJDKDialog {
    private enum FeatureVersion {
        v25("25", 25),
        v26("26", 26);

        final String display;
        final int val;

        FeatureVersion(String display, int val) {
            this.display = display;
            this.val = val;
        }
    }

    private static final String PopupID = "Download JDK";
    private static final ImVec2 DialogSize = new ImVec2(400.0f, 200.0f);
    private static final float ButtonReserve = ImGui.getFrameHeightWithSpacing();
    private static final float Padding = 4.0f;
    private static final float ButtonWidth = 100.0f;
    private static final float ButtonHeight = 30.0f;
    private static final ImVec2 optionSize = new ImVec2();
    private static boolean showDialog = false;
    private static JDKProvider selectedProvider = JDKProvider.Temurin;
    private static FeatureVersion selectedVersion = FeatureVersion.v25;
    private static String message = "";

    static void show() {
        showDialog = true;
        message = "";
        selectedProvider = JDKProvider.Temurin;
        selectedVersion = FeatureVersion.v25;
    }

    static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 center = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(center.x, center.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            renderOptions();
            renderControlButtons();
            ImGui.endPopup();
        }
        if (!ImGui.isPopupOpen(PopupID)) showDialog = false;
    }

    private static void renderOptions() {
        float margin = (ImGui.getContentRegionAvailX() * 0.2f) / 2.0f;
        ImGui.indent(margin);
        ImGui.beginGroup();
        if (!ImGui.beginTable("##DJ_Selection_Layout_Table", 2, ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvailX() - margin)) return;
        ImGui.tableSetupColumn("##DJ_Selection_Label_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##DJ_Selection_Dropdown_COlumn", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableNextColumn();
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (ImGui.getFrameHeightWithSpacing() - ImGui.getTextLineHeightWithSpacing()) / 2.0f);
        ImGui.text("Version:");
        ImGui.tableNextColumn();
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX() - margin);
        if (ImGui.beginCombo("##DJ_Version_Selection_Combo", selectedVersion != null ? selectedVersion.display : "Select version...")) {
            for (FeatureVersion version : FeatureVersion.values()) {
                if (ImGui.selectable(version.display + "##DJ_Version_Selectable_" + version.display, version == selectedVersion)) selectedVersion = version;
            }
            ImGui.endCombo();
        }
        ImGui.popItemWidth();
        ImGui.tableNextColumn();
        ImGui.spacing();
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (ImGui.getFrameHeightWithSpacing() - ImGui.getTextLineHeightWithSpacing()) / 2.0f);
        ImGui.text("Vendor:");
        ImGui.tableNextColumn();
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX() - margin);
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
        ImGui.popItemWidth();
        ImGui.endTable();
        ImGui.endGroup();
        ImGui.getItemRectSize(optionSize);
        ImGui.unindent();
    }

    private static void renderControlButtons() {
        float buttonY = ImGui.getWindowHeight() - ButtonReserve - ButtonHeight / 2.0f;
        float buttonPivotX = ButtonWidth * 0.5f;
        float availX = ImGui.getContentRegionAvailX();
        float downloadX = (availX * 0.25f) - buttonPivotX;
        float cancelX = (availX * 0.75f) - buttonPivotX;
        ImGui.setCursorPos(downloadX, buttonY);
        if (ImGui.button("Download##DJ_Download_Selected_JDK_Button", ButtonWidth, ButtonHeight)) {}
        ImGui.setCursorPos(cancelX, buttonY);
        if (!ImGui.button("Cancel##DJ_Cancel_Button", ButtonWidth, ButtonHeight)) return;
        showDialog = false;
        ImGui.closeCurrentPopup();
        EditEditorPreferencesDialog.closeDownloadJDKDialog();
    }
}

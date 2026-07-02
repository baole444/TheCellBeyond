package editor.dialog;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import utility.IdPool;

import java.util.Arrays;

/**
 * Editor dialogue for editing the user project preferences.
 */
public final class EditProjectSettingsDialog {
    /**
     * Tabs of project preferences
     */
    enum TabName {
        /**
         * General settings.
         */
        General("General"),
        /**
         * Input mapping.
         */
        InputMap("Input Map"),
        /**
         * Script loading.
         */
        Script("Scripts");

        /**
         * Name of the tab.
         */
        final String name;

        /**
         * Create a new {@link TabName} with the given name
         * @param name the new name for the tab
         */
        TabName(String name) {
            this.name = name;
        }

        /**
         * Get the total number of tabs.
         * @return tab count value
         */
        static int size() {
            return values().length;
        }
    }

    private static final IdPool IDPool = new IdPool(0, false);
    private static final String PopupID = "Project Preferences";
    private static final ImVec2 DialogSize = new ImVec2(720.0f, 640.0f);
    private static final float ButtonReserve = ImGui.getFrameHeightWithSpacing();
    private static final float Padding = 4.0f;
    private static boolean showDialog = false;
    private static float tabWidth;
    private static boolean widthCalculated = false;
    private static TabName selectedTab = TabName.General;

    /**
     * Create the dialogue module.
     */
    private EditProjectSettingsDialog() {}

    /**
     * Toggle the show flag for this dialogue.
     */
    public static void show() {
        showDialog = true;
        resetTab();
        InputMapTab.clearInputMapDialogData();
        syncWithProject();
    }

    /**
     * Toggle the show flag for this dialogue and switch to the input mapping tab.
     */
    public static void showToInputMap() {
        show();
        selectedTab = TabName.InputMap;
    }

    /**
     * Toggle the show flag for this dialogue and switch to the scripting tab.
     */
    public static void showToScriptTab() {
        show();
        selectedTab = TabName.Script;
    }

    /**
     * Get the ID pool use by this dialogue.
     * @return the {@link IdPool}
     */
    static IdPool IDPool() {
        return IDPool;
    }

    private static void resetTab() {
        selectedTab = TabName.General;
    }

    private static void syncWithProject() {
        ProjectPreferenceTab.reloadPreferenceData();
        InputMapTab.reloadInputActionData();
    }

    /**
     * Render the dialogue on screen.
     */
    public static void imgui() {
        if (!showDialog) return;
        syncWithProject();
        if (InputMapTab.isShowListeningDialog()) {
            ListenForInputDialog.imgui();
            return;
        }
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            renderTabButtons();
            ImGui.separator();
            float buttonWidth = 120;
            float buttonHeight = 30;
            float regionHeight = ImGui.getContentRegionAvailY() - ButtonReserve - buttonHeight;
            if (ImGui.beginChild("##EPPD_Tab_Region", 0.0f, regionHeight, ImGuiChildFlags.Borders)) renderTabContent();
            ImGui.endChild();
            if (selectedTab == TabName.General) ProjectPreferenceTab.autoSavePreferences();
            renderCloseButton(buttonWidth, buttonHeight);
            ImGui.endPopup();
            IDPool.reset();
        }
        if (!ImGui.isPopupOpen(PopupID)) showDialog = false;
    }

    private static void renderCloseButton(float buttonWidth, float buttonHeight) {
        ImGui.setCursorPosY(ImGui.getWindowHeight() - ButtonReserve - buttonHeight / 2.0f);
        float buttonPivotX = buttonWidth * 0.5f;
        float availX = ImGui.getContentRegionAvailX();
        float closeX = (availX * 0.5f) - buttonPivotX;
        ImGui.setCursorPosX(closeX);
        if (!ImGui.button("Close##EPPD_CLose_Dialog", buttonWidth, buttonHeight)) return;
        showDialog = false;
        ImGui.closeCurrentPopup();
    }

    private static void renderTabContent() {
        switch (selectedTab) {
            case General -> ProjectPreferenceTab.imgui();
            case InputMap -> InputMapTab.imgui();
            case Script -> ScriptsTab.imgui();
        }
    }

    private static void renderTabButtons() {
        if (!ImGui.beginChild("##EPPD_Tabs", 0.0f, ButtonReserve, ImGuiChildFlags.None, ImGuiWindowFlags.NoScrollbar)) {
            ImGui.endChild();
            return;
        }
        if (!widthCalculated) {
            tabWidth = getMaxTabNameWidth();
            widthCalculated = true;
        }
        ImVec2 remainTableSize = ImGui.getContentRegionAvail();
        if (!ImGui.beginTable("##EPPD Tab Buttons", TabName.size(), ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp, remainTableSize)) {
            ImGui.endChild();
            return;
        }
        for (TabName tab : TabName.values()) {
            String id = "##EPPD " + tab.name + " column";
            ImGui.tableSetupColumn(id, ImGuiTableColumnFlags.WidthFixed, tabWidth + Padding);
        }
        ImVec2 availSpace;
        ImVec2 cursorPos;
        TabName pastTab = selectedTab;
        for (TabName tab : TabName.values()) {
            ImGui.tableNextColumn();
            String id = "##EPPD " + tab.name + " tab selectable";
            boolean selected = selectedTab == tab;
            availSpace = ImGui.getContentRegionAvail();
            cursorPos = ImGui.getCursorPos();
            if (ImGui.selectable(id, selected, availSpace)) {
                selectedTab = tab;
                InputMapTab.resetInputMap(pastTab, tab);
            }
            float remainWidth = availSpace.x;
            float textWidth = ImGui.calcTextSizeX(tab.name);
            float offset = Math.max((remainWidth - textWidth) * 0.5f, 0.0f);
            ImGui.setCursorPos(cursorPos.x + offset, cursorPos.y);
            ImGui.text(tab.name);
        }
        ImGui.endTable();
        ImGui.endChild();
    }

    private static float getMaxTabNameWidth() {
        return Arrays.stream(TabName.values())
                .map(tabName -> ImGui.calcTextSizeX(tabName.name))
                .max(Float::compare)
                .orElse(0.0f);
    }
}

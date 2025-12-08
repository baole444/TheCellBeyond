package editor.dialog;

import TheCellBeyond.*;
import editor.EditorIcons;
import editor.ImEditorGui;
import editor.ImGuiLayer;
import imgui.flag.*;
import project.Project;
import project.ProjectData;
import project.ProjectPreference;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Vector2i;
import utility.IdPool;

import java.util.*;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.*;

public class EditProjectSettingsDialog {
    enum TabName {
        General("General"),
        InputMap("Input Map");

        final String name;

        TabName(String name) {
            this.name = name;
        }

        static int size() {
            return values().length;
        }
    }

    private static final IdPool ID_POOL = new IdPool(0, false);
    private static final String POPUP_ID = "Project Preferences";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(720.0f, 640.0f);


    private static final float BUTTON_RESERVE = ImGui.getFrameHeightWithSpacing();
    private static final float SEPARATOR_RESERVE = ImGui.getStyle().getItemSpacingY();
    private static final float padding = 4.0f;
    private static boolean showDialog = false;
    private static final boolean enableBorder = true;
    private static float tabWidth;
    private static boolean widthCalculated = false;
    private static TabName selectedTab = TabName.General;

    public static void show() {
        showDialog = true;
        resetTab();
        InputMapTab.clearInputMapDialogData();
        syncWithProject();
    }

    static IdPool ID_POOL() {
        return ID_POOL;
    }

    private static void resetTab() {
        selectedTab = TabName.General;
    }

    private static void syncWithProject() {
        ProjectPreferenceTab.reloadPreferenceData();
        InputMapTab.reloadInputActionData();
    }

    public static void imgui() {
        if (!showDialog) return;
        syncWithProject();

        if (InputMapTab.isShowListeningDialog()) {
            ListenForInputDialog.imgui();
            return;
        }

        ImGui.openPopup(POPUP_ID);

        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;

        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DIALOG_SIZE);

        if (ImGui.beginPopupModal(POPUP_ID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            renderTabButtons();
            ImGui.separator();

            float buttonWidth = 120;
            float buttonHeight = 30;
            float regionHeight = ImGui.getContentRegionAvailY() - BUTTON_RESERVE - SEPARATOR_RESERVE - buttonHeight - padding;
            if (ImGui.beginChild("##EPPD_Tab_Region", 0.0f, regionHeight, enableBorder)) {
                renderTabContent();
                ImGui.endChild();
            }

            if (selectedTab == TabName.General) ProjectPreferenceTab.autoSavePreferences();

            renderCloseButton(buttonWidth, buttonHeight);

            ImGui.endPopup();
            ID_POOL.reset();
        }

        if (!ImGui.isPopupOpen(POPUP_ID)) showDialog = false;
    }

    private static void renderCloseButton(float buttonWidth, float buttonHeight) {
        ImGui.setCursorPosY(ImGui.getWindowHeight() - BUTTON_RESERVE - (buttonHeight / 2) - ImGui.getStyle().getWindowPaddingY());
        float buttonPivotX = buttonWidth * 0.5f;
        float availX = ImGui.getContentRegionAvailX();
        float cancelX = (availX * 0.5f) - buttonPivotX;
        ImGui.setCursorPosX(cancelX);
        if (ImGui.button("Close##EPPD_CLose_Dialog", buttonWidth, buttonHeight)) {
            showDialog = false;
            ImGui.closeCurrentPopup();
        }
    }

    private static void renderTabContent() {
        switch (selectedTab) {
            case General -> ProjectPreferenceTab.imgui();
            case InputMap -> InputMapTab.imgui();
        }
    }

    private static void renderTabButtons() {
        if (!ImGui.beginChild("##EPPD_Tabs", 0.0f, BUTTON_RESERVE, ImGuiChildFlags.None, ImGuiWindowFlags.NoScrollbar)) {
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
            ImGui.tableSetupColumn(id, ImGuiTableColumnFlags.WidthFixed, tabWidth + padding);
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

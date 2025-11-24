package editor.dialog;

import TheCellBeyond.Input;
import TheCellBeyond.InputAction;
import TheCellBeyond.KeyListener;
import TheCellBeyond.MouseListener;
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

public class EditProjectPreferencesDialog {
    private enum TabName {
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
    private static final int transparentColor = ImGui.colorConvertFloat4ToU32(0.0f, 0.0f, 0.0f, 0.0f);

    private static final float BUTTON_RESERVE = ImGui.getFrameHeightWithSpacing();
    private static final float SEPARATOR_RESERVE = ImGui.getStyle().getItemSpacingY();
    private static final float padding = 4.0f;
    private static boolean showDialog = false;
    private static final boolean enableBorder = true;
    private static float tabWidth;
    private static boolean widthCalculated = false;
    private static TabName selectedTab = TabName.General;

    private static final ImString gameTitle = new ImString(128);
    private static final Vector2i gameWindowSize = new Vector2i(640, 480);
    private static final ImBoolean allowResize = new ImBoolean(false);
    private static final ImBoolean maintainAspectRatio = new ImBoolean(true);
    private static final ImFloat textureGlobalScale = new ImFloat(1.0f);

    private static final Map<String, InputAction> inputActions = new HashMap<>();
    private static final ImString actionNameSearchFilter = new ImString(128);
    private static final List<Integer> actionKeySearchFilter = new ArrayList<>();
    private static final Set<Integer> currentMods = new HashSet<>();
    private static boolean filterChanged = true;
    private static boolean listeningInput = false;


    public static void show() {
        showDialog = true;
        clearInputFilter();
        resetTab();
        loadFromPreference();
        loadInputActions();
    }

    private static void resetTab() {
        selectedTab = TabName.General;
    }

    public static void imgui() {
        if (!showDialog) return;

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

            ImGui.setCursorPosY(ImGui.getWindowHeight() - BUTTON_RESERVE - (buttonHeight / 2) - ImGui.getStyle().getWindowPaddingY());
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float applyX = (availX * 0.15f) - buttonPivotX;
            float saveX = (availX * 0.5f) - buttonPivotX;
            float cancelX = (availX * 0.85f) - buttonPivotX;
            boolean canModify = gameTitle.isNotEmpty() && gameWindowSize.x > 0 && gameWindowSize.y > 0;
            ImGui.setCursorPosX(applyX);
            if (canModify) {
                if (ImGui.button("Apply", buttonWidth, buttonHeight)) savePreference();
            } else {
                ImGui.beginDisabled();
                ImGui.button("Apply", buttonWidth, buttonHeight);
                ImGui.endDisabled();
            }

            ImGui.sameLine();
            ImGui.setCursorPosX(saveX);
            if (canModify) {
                if (ImGui.button("Save", buttonWidth, buttonHeight)) {
                    savePreference();
                    showDialog = false;
                    ImGui.closeCurrentPopup();
                }
            } else {
                ImGui.beginDisabled();
                ImGui.button("Save", buttonWidth, buttonHeight);
                ImGui.endDisabled();
            }

            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, buttonHeight)) {
                showDialog = false;
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
            ID_POOL.reset();
        }

        if (!ImGui.isPopupOpen(POPUP_ID)) showDialog = false;
    }

    private static void renderTabContent() {
        switch (selectedTab) {
            case General -> renderGeneral();
            case InputMap -> renderInputMap();
        }
    }

    private static void renderGeneral() {
        ImGui.text("Title:");
        ImGui.inputTextWithHint("##Game title", "Enter a name for the project...", gameTitle);
        if (gameTitle.isEmpty()) {
            ImGui.textColored(ImGui.colorConvertFloat4ToU32(1.0f, 0.2f, 0.2f, 1.0f), "Game title cannot be empty");
        } else {
            ImGui.newLine();
        }

        ImGui.spacing();
        ImGui.text("Window size:");
        ImGui.beginDisabled();
        ImGui.textWrapped("Determine the default size of game window");
        ImGui.endDisabled();
        gameWindowSize.x = inputInt("Width", gameWindowSize.x, 1);
        gameWindowSize.y = inputInt("Height", gameWindowSize.y, 1);
        ImGui.spacing();

        ImGui.checkbox("Resizable", allowResize);
        ImGui.beginDisabled();
        ImGui.textWrapped("(allow player to resize the game window)");
        ImGui.endDisabled();
        ImGui.spacing();

        ImGui.checkbox("Lock aspect ratio", maintainAspectRatio);
        ImGui.beginDisabled();
        ImGui.textWrapped("(maintain the game's intended aspect ratio when window is resized)");
        ImGui.endDisabled();
        ImGui.spacing();

        ImGui.text("Texture :");
        ImGui.beginDisabled();
        ImGui.textWrapped("Settings that affect the appearance of texture, project-wise.");
        ImGui.endDisabled();
        textureGlobalScale.set(inputFloat("Global Scaling", textureGlobalScale.get(), 0.01f));
    }

    private static void renderInputMap() {
        if (!ImGui.beginTable("EPPD_InputMap_Search", 4, ImGui.getContentRegionAvailX())) return;
        float columnWidth = (ImGui.getContentRegionAvailX() - ImGui.calcTextSizeX("+X Clear All+")) / 2.0f;
        ImGui.tableSetupColumn("EPPD_InputMap_Search_Name_Column", ImGuiTableColumnFlags.WidthFixed, columnWidth);
        ImGui.tableSetupColumn("EPPD_InputMap_Name_Clear_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("EPPD_InputMap_Search_Key_Column", ImGuiTableColumnFlags.WidthFixed, columnWidth * 0.9f);
        ImGui.tableSetupColumn("EPPD_InputMap_Clear_All_Column", ImGuiTableColumnFlags.WidthFixed);

        ImGui.tableNextColumn();
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        if (ImGui.inputTextWithHint("##EPPD_Filter_Action_Name_Input", "Filter by name...", actionNameSearchFilter, ImGuiInputTextFlags.EscapeClearsAll)) filterChanged = true;
        ImGui.popItemWidth();

        ImGui.tableNextColumn();
        ImGui.pushStyleColor(ImGuiCol.Button, transparentColor);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, transparentColor);
        if (ImGui.button("X##EPPD_Clear_Action_Name_Filter")) {
            actionNameSearchFilter.clear();
            filterChanged = true;
        }
        ImGui.popStyleColor(2);

        ImGui.tableNextColumn();
        renderInputFilter();

        ImGui.tableNextColumn();
        if (ImGui.button("Clear All##EPPD_Clear_All_Filter")) clearInputFilter();

        ImGui.endTable();
    }

    private static void renderInputFilter() {
        String displayText = getFilterInputKeyName();
        String hint = listeningInput ? "Listening for input..." : "Filter by input...";

        ImString display = new ImString(displayText, 128);
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        ImGui.inputTextWithHint("##EPPD_Filtered_Action_Key_Input", hint, display, ImGuiInputTextFlags.ReadOnly);
        ImGui.popItemWidth();

        boolean isActive = ImGui.isItemActive();
        if (isActive && !listeningInput) {
            ImGuiLayer.prioritizeEngineInputCallback(true);
            listeningInput = true;
            currentMods.clear();
            actionKeySearchFilter.clear();
        }

        if (!isActive && listeningInput) {
            ImGuiLayer.prioritizeEngineInputCallback(false);
            listeningInput = false;
        }

        if (listeningInput) recordFilterInput();
    }

    private static String getFilterInputKeyName() {
        if (actionKeySearchFilter.isEmpty()) return "";

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < actionKeySearchFilter.size(); i++) {
            if (i > 0) builder.append(" + ");
            int keyCode = actionKeySearchFilter.get(i);
            String name = Input.getKeyName(keyCode);
            builder.append(name != null ? name : "Unknown");
        }

        return builder.toString();
    }

    private static void recordFilterInput() {
        Set<Integer> newMods = new HashSet<>();
        for (int keyCode : Input.getModifierKeysCodes()) {
            if (KeyListener.isKeyPressed(keyCode)) {
                newMods.add(keyCode);
            }
        }

        for (int keyCode : KeyListener.getTappedKeyCode()) {
            if (Input.isModifierKey(keyCode)) continue;

            actionKeySearchFilter.clear();
            actionKeySearchFilter.addAll(newMods);
            actionKeySearchFilter.add(keyCode);
            filterChanged = true;
            return;
        }

        for (int keyCode : MouseListener.getPressedButtons()) {
            if (MouseListener.isDragging()) return;
            actionKeySearchFilter.clear();
            actionKeySearchFilter.addAll(newMods);
            actionKeySearchFilter.add(keyCode);
            filterChanged = true;
            return;
        }

        if (!newMods.equals(currentMods)) {
            currentMods.clear();
            currentMods.addAll(newMods);

            if (!newMods.isEmpty()) {
                actionKeySearchFilter.clear();
                actionKeySearchFilter.addAll(newMods);
                filterChanged = true;
            }
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
                resetInputMap(pastTab, tab);
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

    private static void resetInputMap(TabName pastTab, TabName selectedTab) {
        if (pastTab == TabName.InputMap && pastTab == selectedTab) return;
        if (pastTab != TabName.InputMap && selectedTab == TabName.InputMap) {
            loadInputActions();
            clearInputFilter();
            return;
        }

        if (pastTab == TabName.InputMap) {
            clearInputFilter();
            inputActions.clear();
        }
    }

    private static void clearInputFilter() {
        filterChanged = true;
        listeningInput = false;
        ImGuiLayer.prioritizeEngineInputCallback(false);
        actionKeySearchFilter.clear();
        actionNameSearchFilter.clear();
        currentMods.clear();
    }

    private static int inputInt(String label, int target, int minValue) {
        String id = label + "_" + ID_POOL.newId();
        ImGui.pushID(id);
        final boolean modified;
        final ImInt destination = new ImInt(target);

        modified = ImGui.inputInt(label, destination);

        if (modified) target = Math.max(destination.get(), minValue);

        ImGui.popID();
        return target;
    }

    private static float inputFloat(String label, float target, float minValue) {
        String id = label + "_" + ID_POOL.newId();
        ImGui.pushID(id);
        final boolean modified;
        final ImFloat destination = new ImFloat(target);

        modified = ImGui.inputFloat(label, destination);

        if (modified) target = Math.max(destination.get(), minValue);

        ImGui.popID();
        return target;
    }

    private static void loadFromPreference() {
        ProjectPreference preference = Project.preference();
        gameTitle.set(preference.name());
        gameWindowSize.set(preference.gameWindowWidth(), preference.gameWindowHeight());
        allowResize.set(preference.allowResize());
        maintainAspectRatio.set(preference.maintainAspectRatio());
        textureGlobalScale.set(preference.textureGlobalScale());
    }

    private static void loadInputActions() {
        ProjectData project = Project.currentProject();

        if (project == null || project.inputActions() == null) return;
        inputActions.clear();
        inputActions.putAll(project.inputActions());
    }

    private static void savePreference() {
        if (gameTitle.isEmpty()) gameTitle.set("Untitled Game");

        int width = Math.max(1, gameWindowSize.x);
        int height = Math.max(1, gameWindowSize.y);
        float scale = Math.max(0.01f, textureGlobalScale.get());

        Project.updateProjectPreference(gameTitle.get(),
                width, height,
                allowResize.get(), maintainAspectRatio.get(),
                scale
        );
    }

    private static float getMaxTabNameWidth() {
        return Arrays.stream(TabName.values())
                .map(tabName -> ImGui.calcTextSizeX(tabName.name))
                .max(Float::compare)
                .orElse(0.0f);
    }
}

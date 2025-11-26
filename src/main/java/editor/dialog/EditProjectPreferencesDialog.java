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
    private static boolean projectPreferencesChanged = false;

    private static final Map<String, InputAction> inputActions = new HashMap<>();
    private static final ImString newActionName = new ImString(128);
    private static final ImString actionNameSearchFilter = new ImString(128);
    private static final List<Integer> actionKeySearchFilter = new ArrayList<>();
    private static final Set<Integer> currentMods = new HashSet<>();
    private static boolean filterChanged = true;
    private static boolean listeningInput = false;

    private static final Map<String, InputAction> filteredInputActions = new LinkedHashMap<>();

    public static void show() {
        projectPreferencesChanged = false;
        showDialog = true;
        newActionName.clear();
        clearInputFilter();
        resetTab();
        loadFromPreference();
        loadInputActions();
    }

    private static void resetTab() {
        selectedTab = TabName.General;
    }

    private static void syncWithProject() {
        loadFromPreference();
        loadInputActions();
    }

    public static void imgui() {
        if (!showDialog) return;
        syncWithProject();

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

            if (projectPreferencesChanged) {
                autoSavePreferences();
                projectPreferencesChanged = false;
            }

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
        if (!ImGui.beginTable("EPPD_InputMap_Search_Table", 3, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingFixedFit,ImGui.getContentRegionAvailX())) return;
        float columnWidth = (ImGui.getContentRegionAvailX() - ImGui.calcTextSizeX("+Clear All+") - padding) / 2.0f;
        ImGui.tableSetupColumn("EPPD_InputMap_Search_Name_Column", ImGuiTableColumnFlags.WidthFixed, columnWidth);
        ImGui.tableSetupColumn("EPPD_InputMap_Search_Key_Column", ImGuiTableColumnFlags.WidthFixed, columnWidth * 0.9f);
        ImGui.tableSetupColumn("EPPD_InputMap_Clear_All_Column", ImGuiTableColumnFlags.WidthStretch);

        ImGui.tableNextColumn();
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX() - ImGui.calcTextSizeX("+X+"));
        if (ImGui.inputTextWithHint("##EPPD_Filter_Action_Name_Input", "Filter by name...", actionNameSearchFilter, ImGuiInputTextFlags.EscapeClearsAll)) filterChanged = true;
        ImGui.popItemWidth();
        ImGui.sameLine();
        renderClearButton("##EPPD_Clear_Action_Name_Filter", "Clear action name filter", () -> {
            actionNameSearchFilter.clear();
            filterChanged = true;
        });

        ImGui.tableNextColumn();
        renderInputFilter();

        ImGui.tableNextColumn();
        if (ImGui.button("Clear All##EPPD_Clear_All_Filter", ImGui.getContentRegionAvailX(), 0.0f)) clearInputFilter();

        ImGui.endTable();

        ImGui.separator();
        if (!ImGui.beginTable("EPPD_InputMap_New_Action_Table", 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingFixedFit, ImGui.getContentRegionAvailX())) return;
        ImGui.tableSetupColumn("EPPD_New_Action_Name_Input_Column", ImGuiTableColumnFlags.WidthFixed, columnWidth * 1.9f + ImGui.getStyle().getItemSpacingX());
        ImGui.tableSetupColumn("EPPD_Create_New_Action_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableNextColumn();
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX() - ImGui.calcTextSizeX("+X+"));
        ImGui.inputTextWithHint("##EPPD_New_Action_Name_Input", "Create new Action...", newActionName);
        ImGui.popItemWidth();
        ImGui.sameLine();
        renderClearButton("##EPPD_Clear_New_Action_Name", "Clear new action name", newActionName::clear);

        boolean canAdd = canCreateNewAction(newActionName.get());
        ImGui.tableNextColumn();
        if (!canAdd) ImGui.beginDisabled();
        if (ImGui.button("Create New##EPPD_Create_New_Action_Button", ImGui.getContentRegionAvailX(), 0.0f)) {
            String actionName = newActionName.get().trim();
            if (Project.addInputAction(actionName, new ArrayList<>())) {
                newActionName.clear();
                filterChanged = true;
            }
        }
        if (!canAdd) ImGui.endDisabled();
        ImGui.endTable();

        ImGui.separator();
        if (!ImGui.beginChild("##EPPD_InputAction_List_region", ImGui.getContentRegionAvail(), false)) return;
        renderInputActionList();
        ImGui.endChild();
    }

    private static void renderInputActionList() {
        if (!ImGui.beginTable("##EPPD_InPutAction_List_Table", 3, ImGuiTableFlags.SizingFixedFit, ImGui.getContentRegionAvailX())) return;
        ImGui.tableSetupColumn("##EPPD_InputAction_Content_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("##EPPD_InputAction_Control_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##EPPD_InputAction_Delete_Column", ImGuiTableColumnFlags.WidthFixed);

        if (filterChanged) updateFilteredInputActions();

        for (Map.Entry<String, InputAction> entry : filteredInputActions.entrySet()) {
            String actionName = entry.getKey();
            InputAction action = entry.getValue();

            ImGui.tableNextColumn();
            String headerId = actionName + "##EPPD_Action_Header_" + actionName;
            boolean opened = ImGui.collapsingHeader(headerId);

            ImGui.tableNextColumn();
            String addId = "Add##EPPD_Add_KeyCombo_" + actionName;
            if (ImEditorGui.iconButton(addId, EditorIcons.Icons.New, "Add new key combo")) {

            }

            ImGui.tableNextColumn();
            String deleteActionId = "Delete##EPPD_Delete_Action_" + actionName;
            if (ImEditorGui.iconButton(deleteActionId, EditorIcons.Icons.Delete, "Delete this action")) {
                if (Project.removeInputAction(actionName)) filterChanged = true;
            }

            if (!opened) continue;
            List<Set<InputKey>> keyCombos = action.keys();
            if (keyCombos.isEmpty()) continue;
            for (int i = 0; i < keyCombos.size(); i++) {
                Set<InputKey> combo = keyCombos.get(i);
                String displayText = keyCodeComboToString(combo);
                ImGui.tableNextColumn();
                ImGui.indent();
                ImGui.text(displayText);
                ImGui.unindent();

                ImGui.tableNextColumn();
                String editId = "Edit##EPPD_Edit_KeyCombo_" + actionName + "_" + i;
                if (ImEditorGui.iconButton(editId, EditorIcons.Icons.Edit, "Edit this key combo")) {

                }

                ImGui.tableNextColumn();
                String deleteComboId = "Delete##EPPD_Delete_KeyCombo_" + actionName + "_" + i;
                if (ImEditorGui.iconButton(deleteComboId, EditorIcons.Icons.Delete, "Delete this key combo")) {
                    List<Set<InputKey>> update = new ArrayList<>();
                    for (Set<InputKey> keyCombo : keyCombos) {
                        if (keyCombo == combo) continue;
                        update.add(keyCombo);
                    }
                    Project.updateInputActionKey(actionName, update);
                }
            }
        }

        ImGui.endTable();
    }

    private static void renderInputFilter() {
        String displayText = getFilterInputKeyName();
        String hint = listeningInput ? "Listening for input..." : "Filter by input...";

        ImString display = new ImString(displayText, 128);
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX() - ImGui.calcTextSizeX("+X+"));
        ImGui.inputTextWithHint("##EPPD_Filtered_Action_Key_Input", hint, display, ImGuiInputTextFlags.ReadOnly);
        ImGui.popItemWidth();

        boolean isActive = ImGui.isItemActive();
        if (isActive && !listeningInput) {
            ImGuiLayer.prioritizeEngineInputCallback(true);
            listeningInput = true;
            currentMods.clear();
            actionKeySearchFilter.clear();
        }

        ImGui.sameLine();
        renderClearButton("##EPPD_Clear_Action_Key_Filter", "Clear action key filter", () -> {
            listeningInput = false;
            actionKeySearchFilter.clear();
            currentMods.clear();
            filterChanged = true;
        });

        if (!isActive && listeningInput) {
            ImGuiLayer.prioritizeEngineInputCallback(false);
            listeningInput = false;
        }

        if (listeningInput) recordFilterInput();
    }

    private static void renderClearButton(String id, String hint, Runnable onClear) {
        ImGui.beginGroup();
        ImGui.pushStyleColor(ImGuiCol.Button, transparentColor);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, transparentColor);
        if (ImGui.button("X" + id)) onClear.run();
        ImGui.popStyleColor(2);
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text(hint);
            ImGui.endTooltip();
        }
        ImGui.endGroup();
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

    private static void autoSavePreferences() {
        if (gameTitle.isEmpty() || gameWindowSize.x <= 0 || gameWindowSize.y <= 0) return;

        int width = gameWindowSize.x;
        int height = gameWindowSize.y;
        float scale = Math.max(0.01f, textureGlobalScale.get());

        Project.updateProjectPreference(gameTitle.get(),
                width, height,
                allowResize.get(), maintainAspectRatio.get(),
                scale
        );
    }

    private static void updateFilteredInputActions() {
        filteredInputActions.clear();

        String filterName = actionNameSearchFilter.get().toLowerCase().trim();
        boolean filterByName = !actionNameSearchFilter.isEmpty();
        boolean filterByKey = !actionKeySearchFilter.isEmpty();

        for (Map.Entry<String, InputAction> entry : inputActions.entrySet()) {
            String name = entry.getKey();
            InputAction action = entry.getValue();
            if (filterByName && !name.toLowerCase().contains(filterName)) continue;
            if (filterByKey && !hasFilteredKey(action)) continue;
            filteredInputActions.put(name, action);
        }

        filterChanged = false;
    }

    private static boolean hasFilteredKey(InputAction action) {
        if (action.keys().isEmpty()) return false;
        HashSet<Integer> filterKeys = new HashSet<>(actionKeySearchFilter);
        for (Set<InputKey> keyCombo : action.keys()) {
            if (keyCombo.isEmpty()) continue;
            Set<Integer> keyCodes = keyCombo.stream().map(InputKey::code).collect(Collectors.toSet());

            if (filterKeys.equals(keyCodes)) return true;
        }

        return false;
    }

    private static String keyCodeComboToString(Set<InputKey> combo) {
        if (combo.isEmpty()) return "";

        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (InputKey key : combo) {
            if (i > 0) builder.append(" + ");
            String name = Input.getKeyName(key.code());
            builder.append(name != null ? name : "Unknown");
            i++;
        }

        return builder.toString();
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

    private static boolean canCreateNewAction(String newNew) {
        if (newNew == null || newNew.isBlank()) return false;

        String name = newNew.trim();
        if (name.isEmpty()) return false;

        if (Project.currentProject() == null) return false;
        Map<String, InputAction> actions = Project.currentProject().inputActions();
        if (actions == null) return true;

        return !actions.containsKey(name);
    }

    private static void resetInputMap(TabName pastTab, TabName selectedTab) {
        if (pastTab == TabName.InputMap && pastTab == selectedTab) return;
        if (pastTab != TabName.InputMap && selectedTab == TabName.InputMap) {
            newActionName.clear();
            loadInputActions();
            clearInputFilter();
            return;
        }

        if (pastTab == TabName.InputMap) {
            clearInputFilter();
            inputActions.clear();
            newActionName.clear();
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

    private static float getMaxTabNameWidth() {
        return Arrays.stream(TabName.values())
                .map(tabName -> ImGui.calcTextSizeX(tabName.name))
                .max(Float::compare)
                .orElse(0.0f);
    }
}

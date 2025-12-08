package editor.dialog;

import TheCellBeyond.*;
import editor.EditorIcons;
import editor.ImEditorGui;
import editor.ImGuiLayer;
import imgui.ImGui;
import imgui.flag.*;
import imgui.type.ImString;
import project.Project;
import project.ProjectData;

import java.util.*;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.*;

class InputMapTab {
    private static final int transparentColor = ImGui.colorConvertFloat4ToU32(0.0f, 0.0f, 0.0f, 0.0f);
    private static final float padding = 4.0f;

    private static final Map<String, InputAction> inputActions = new HashMap<>();
    private static final ImString newActionName = new ImString(128);
    private static final ImString actionNameSearchFilter = new ImString(128);
    private static final List<Integer> actionKeySearchFilter = new ArrayList<>();
    private static final Set<Integer> currentMods = new HashSet<>();
    private static boolean filterChanged = true;
    private static boolean listeningInput = false;
    private static String editingActionName;
    private static final ImString editingActionNameBuffer = new ImString(128);
    private static final Map<String, InputAction> filteredInputActions = new LinkedHashMap<>();

    private static boolean showListenForInputDialog = false;
    private static KeyComboCallback pendingCallback = null;

    static boolean isShowListeningDialog() {
        return showListenForInputDialog;
    }

    static void clearInputMapDialogData() {
        newActionName.clear();
        clearInputFilter();
    }

    static void reloadInputActionData() {
        ProjectData project = Project.currentProject();

        if (project == null || project.inputActions() == null) return;
        if (!inputActions.equals(project.inputActions())) filterChanged = true;

        inputActions.clear();
        inputActions.putAll(project.inputActions());
    }

    static void closeListForInputDialog() {
        showListenForInputDialog = false;
        pendingCallback = null;
    }

    static void resetInputMap(EditProjectSettingsDialog.TabName pastTab, EditProjectSettingsDialog.TabName selectedTab) {
        if (pastTab == EditProjectSettingsDialog.TabName.InputMap && pastTab == selectedTab) return;
        if (pastTab != EditProjectSettingsDialog.TabName.InputMap && selectedTab == EditProjectSettingsDialog.TabName.InputMap) {
            newActionName.clear();
            reloadInputActionData();
            clearInputFilter();
            return;
        }

        if (pastTab == EditProjectSettingsDialog.TabName.InputMap) {
            clearInputFilter();
            inputActions.clear();
            newActionName.clear();
        }
    }

    static void clearInputFilter() {
        filterChanged = true;
        listeningInput = false;
        ImGuiLayer.prioritizeEngineInputCallback(false);
        actionKeySearchFilter.clear();
        actionNameSearchFilter.clear();
        currentMods.clear();
    }

    static void imgui() {
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

            if (renderActionNameEdit(actionName)) continue;

            String headerId = actionName + "##EPPD_Action_Header_" + actionName;
            boolean opened = ImGui.collapsingHeader(headerId, ImGuiTreeNodeFlags.DefaultOpen);

            if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(GLFW_MOUSE_BUTTON_1)) {
                editingActionName = actionName;
                editingActionNameBuffer.set(actionName);
                ImGui.tableNextRow();
                continue;
            }

            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.text("Double click to edit action's name");
                ImGui.endTooltip();
            }

            ImGui.tableNextColumn();
            String addId = "Add##EPPD_Add_KeyCombo_" + actionName;
            if (ImEditorGui.iconButton(addId, EditorIcons.Icons.New, "Add new key combo")) {
                final String finalName = actionName;
                pendingCallback = (resultKeyCombo, accepted) -> {
                    if (!accepted || resultKeyCombo == null || resultKeyCombo.isEmpty()) return;
                    List<Set<InputKey>> current = new ArrayList<>(action.keys());
                    current.add(resultKeyCombo);
                    Project.updateInputActionKey(finalName, current);
                };
                showListenForInputDialog = true;
                ListenForInputDialog.show(pendingCallback);
            }

            ImGui.tableNextColumn();
            String deleteActionId = "Delete##EPPD_Delete_Action_" + actionName;
            if (ImEditorGui.iconButton(deleteActionId, EditorIcons.Icons.Delete, "Delete this action")) {
                if (Project.removeInputAction(actionName)) filterChanged = true;
            }

            if (!opened) continue;
            List<Set<InputKey>> keyCombos = action.keys();
            if (keyCombos.isEmpty()) continue;
            renderKeyCombos(keyCombos, actionName);
        }

        ImGui.endTable();
    }

    private static boolean renderActionNameEdit(String actionName) {
        if (!Objects.equals(editingActionName, actionName)) return false;
        ImGui.setKeyboardFocusHere();
        ImGui.inputTextWithHint("##EPPD_Edit_Action_Name_" + actionName, "Enter new name for action...", editingActionNameBuffer);

        if (ImGui.isItemFocused() && ImGui.isKeyPressed(GLFW_KEY_ESCAPE)) {
            editingActionNameBuffer.clear();
            editingActionName = null;
            return true;
        }

        if (!ImGui.isItemFocused() || !ImGui.isKeyPressed(GLFW_KEY_ENTER)) {
            ImGui.tableNextRow();
            return true;
        }

        String newName = editingActionNameBuffer.get().trim();
        if (!newName.isEmpty() && !newName.equals(actionName) && !inputActions.containsKey(newName)) {
            if (Project.updateInputActionName(actionName, newName)) {
                filterChanged = true;
            }
        }

        editingActionNameBuffer.clear();
        editingActionName = null;
        return true;
    }

    private static void renderKeyCombos(List<Set<InputKey>> keyCombos, String actionName) {
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
                final String finalName = actionName;
                final int index = i;
                pendingCallback = (resultKeyCombo, accepted) -> {
                    if (!accepted || resultKeyCombo == null ||  resultKeyCombo.isEmpty()) return;
                    List<Set<InputKey>> current = new ArrayList<>(keyCombos);
                    current.set(index, resultKeyCombo);
                    Project.updateInputActionKey(finalName, current);
                };
                showListenForInputDialog = true;
                ListenForInputDialog.show(pendingCallback, combo);
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

        if (newMods.equals(currentMods)) return;
        currentMods.clear();
        currentMods.addAll(newMods);

        if (newMods.isEmpty()) return;
        actionKeySearchFilter.clear();
        actionKeySearchFilter.addAll(newMods);
        filterChanged = true;
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
}

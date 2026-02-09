package editor;

import TheCellBeyond.InputAction;
import components.Controller2D;
import components.ControllerBinding;
import components.ControllerDirection;
import components.InputActivation;
import editor.dialog.EditProjectSettingsDialog;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiComboFlags;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImString;
import org.joml.Vector2f;
import project.Project;

import java.util.*;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.*;

class ControllerBindingEditor {
    private static final String CONTROL_SECTION = "##Controller bindings controls";
    private static Controller2D editingController;
    private static String selectedName;

    private static String editingName;
    private static final ImString editingNameBuffer = new ImString(256);
    private static final ImString actionNameSearchFilter = new ImString(256);
    private static final Map<String, InputAction> filteredInputActions = new LinkedHashMap<>();
    private static boolean wantToEditInputMap = false;

    private static final float CONTROL_RESERVE = ImGui.getFrameHeightWithSpacing();
    private static final float bindingListXPercentage = 0.25f;
    private static final float bindingPropertiesXPercentage = 0.35f;
    private static final float bindingListWidth = 120.0f;
    private static final float bindingPropertiesWidth = 180.0f;
    private static final float padding = 4.0f;

    static void edit(Controller2D controller2D) {
        if (controller2D != editingController) {
            clearDialogData();
            editingController = controller2D;
        }
    }

    static void imgui() {
        if (editingController == null) {
            ImGui.textWrapped("Selected a Controller2D component from Inspector panel to start editing its details");
            return;
        }

        if (editingController.gameObject == null || editingController.gameObject.isRemoved() || editingController.getUUID() == null) {
            clearDialogData();
            return;
        }

        if (wantToEditInputMap) {
            wantToEditInputMap = false;
            EditProjectSettingsDialog.showToInputMap();
            return;
        }

        if (!ImGui.beginTable("##CBE_layout_table", 2, ImGuiTableFlags.NoBordersInBody | ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvail())) return;
        float remainWidth = Math.max(bindingListWidth, ImGui.getContentRegionAvailX() * bindingListXPercentage);
        ImGui.tableSetupColumn("##CBE_BindingList_Column", ImGuiTableColumnFlags.WidthFixed, remainWidth);
        ImGui.tableSetupColumn("##CBE_BindingConfigs_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableNextColumn();
        if (ImGui.beginChild(CONTROL_SECTION, 0, CONTROL_RESERVE + padding, false)) renderControllerControl();
        ImGui.endChild();

        if (ImGui.beginChild("##CBE_BindingList", ImGui.getContentRegionAvail(), true)) renderBindingList();
        ImGui.endChild();

        ImGui.tableNextColumn();
        if (ImGui.beginChild("##CBE_BindingConfigs", ImGui.getContentRegionAvail(), true)) {
            if (selectedName == null) {
                ImGui.beginDisabled();
                ImGui.textWrapped("Select a controller binding on the left panel or create a new binding to start editing its properties");
                ImGui.endDisabled();
            } else {
                renderBindingContent();
            }
        }

        ImGui.endChild();

        ImGui.endTable();
    }

    private static void renderControllerControl() {
        if (EditorWidget.iconButton("Add##Add_New_ControllerBinding_CBE", EditorIcons.Icons.New, "Create new binding")) editingController.newBinding();
        if (selectedName == null) return;

        ImGui.sameLine();
        if (EditorWidget.iconButton("Duplicate##Duplicate_ControllerBinding_CBE", EditorIcons.Icons.Copy, "Duplicate selected binding")) editingController.duplicateBinding(selectedName);

        ImGui.sameLine();
        if (EditorWidget.iconButton("Delete##Delete_ControllerBinding_CBE", EditorIcons.Icons.Delete, "Delete selected binding")) {
            editingController.removeBinding(selectedName);
            if (Objects.equals(editingName, selectedName)) {
                editingName = null;
                editingNameBuffer.clear();
            }
            selectedName = null;
        }
    }

    private static void renderBindingList() {
        HashMap<String, ControllerBinding> bindings = editingController.getBindings();

        for (Map.Entry<String, ControllerBinding> entry : bindings.entrySet()) {
            boolean isSelected = Objects.equals(selectedName, entry.getKey());
            ImVec2 cursorPos = ImGui.getCursorPos();
            float height = ImGui.getTextLineHeight() + ImGui.getStyle().getWindowPaddingY() * 2;
            ImGui.setNextItemAllowOverlap();
            if (ImGui.selectable("##" + entry.getKey(), isSelected, 0.0f, height) && !isSelected) selectedName = entry.getKey();

            if (renderBindingNameEdit(entry.getKey(), cursorPos, isSelected)) continue;

            if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(GLFW_MOUSE_BUTTON_1)) {
                editingName = entry.getKey();
                editingNameBuffer.set(editingName);
                ImGui.spacing();
                continue;
            }

            ImGui.setCursorPos(cursorPos.x, cursorPos.y + (height - ImGui.getTextLineHeight()) / 2.0f);
            ImGui.text(entry.getKey());
            ImGui.spacing();
        }
    }

    private static boolean renderBindingNameEdit(String name, ImVec2 cursorPos, boolean isSelected) {
        if (!Objects.equals(editingName, name)) return false;
        ImGui.setCursorPos(cursorPos);
        ImGui.setKeyboardFocusHere();
        ImGui.inputText("##CBE" + "Edit_" + name, editingNameBuffer);
        if (ImGui.isItemFocused() && ImGui.isKeyPressed(GLFW_KEY_ESCAPE)) {
            editingNameBuffer.clear();
            editingName = null;
            ImGui.spacing();
            return true;
        }

        if ((ImGui.isItemFocused() && ImGui.isKeyPressed(GLFW_KEY_ENTER)) || !isSelected) {
            String newName = editingNameBuffer.get().trim();
            boolean success = editingController.renameBinding(name, newName);
            if (success) selectedName = newName;
            editingNameBuffer.clear();
            editingName = null;
            ImGui.spacing();
            return true;
        }

        ImGui.spacing();
        return true;
    }

    private static void renderBindingContent() {
        if (!ImGui.beginTable("##CBE_Binding_Config_Table", 2, ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvail())) return;
        float remainWidth = Math.max(bindingPropertiesWidth, ImGui.getContentRegionAvailX() * bindingPropertiesXPercentage);
        ImGui.tableSetupColumn("##CBE_Binding_Properties_Column", ImGuiTableColumnFlags.WidthFixed, remainWidth);
        ImGui.tableSetupColumn("##CBE_Binding_InputAction_Column", ImGuiTableColumnFlags.WidthStretch);

        ImGui.tableNextColumn();
        renderBindingProperties();
        ImGui.tableNextColumn();
        renderBindingActions();
        ImGui.endTable();
    }

    private static void renderBindingProperties() {
        ControllerBinding binding = editingController.getBinding(selectedName);
        if (binding == null) {
            ImGui.textWrapped("Selected binding does not exist for this controller");
            return;
        }

        if (!ImGui.beginChild("#CBE_Binding_Properties_Region", ImGui.getContentRegionAvail(), true)) {
            ImGui.endChild();
            return;
        }
        Vector2f dir = new Vector2f(binding.directionVector());
        if (EditorWidget.dragVec2Ctrl("Direction", dir, 0.0f, 0.1f, binding)) binding.direction.set(dir);
        if (ImGui.beginCombo("##Set_Direction_Using_Common_Direction_Combo_CBE", "Use common direction")) {
            if (ImGui.selectable("UP (x:0.0, y:1.0)##Select_UP_Dir_Selectable_CBE")) binding.direction.set(ControllerDirection.up());
            if (ImGui.selectable("DOWN (x:0.0, y:-1.0)##Select_DOWN_Dir_Selectable_CBE")) binding.direction.set(ControllerDirection.down());
            if (ImGui.selectable("LEFT (x:-1.0, y:0.0)##Select_LEFT_Dir_Selectable_CBE")) binding.direction.set(ControllerDirection.left());
            if (ImGui.selectable("RIGHT (x:1.0, y:0.0)##Select_RIGHT_Dir_Selectable_CBE")) binding.direction.set(ControllerDirection.right());
            ImGui.endCombo();
        }

        ImGui.separator();
        ImGui.text("Activation Mode:");
        InputActivation.ActivationMode currentMode = binding.activationMode();
        if (ImGui.beginCombo("##Select_Binding_Activation_Mode_Combo_CBE", currentMode.name())) {
            for (InputActivation.ActivationMode mode : InputActivation.ActivationMode.values()) {
                String label = mode.name() + "##Select_" + mode.name() + "_ActivationMode_Selectable_CBE";
                if (ImGui.selectable(label, currentMode == mode)) binding.activation.activationMode(mode);
            }

            ImGui.endCombo();
        }

        if (binding.activationMode() == InputActivation.ActivationMode.Hold) {
            ImGui.indent();
            float holdTime = EditorWidget.dragFloatCtrl("Hold Time", binding.activation.requiredHoldTime(), 0.0f, 0.1f, binding, 0.0f);
            ImGui.unindent();
            if (Float.compare(holdTime, binding.activation.requiredHoldTime()) != 0) binding.activation.requiredHoldTime(holdTime);
        }

        ImGui.endChild();
    }

    private static void renderBindingActions() {
        ControllerBinding binding = editingController.getBinding(selectedName);
        if (binding == null) {
            ImGui.textWrapped("Selected binding does not exist for this controller");
            return;
        }
        ImGui.text("Bind Input Action:");
        ImGui.sameLine();
        renderInputActionSelectionCombo(binding);

        ImGui.spacing();
        if (!ImGui.beginChild("##CBE_Bound_Action_List_Display_Region", ImGui.getContentRegionAvail(), true)) {
            ImGui.endChild();
            return;
        }

        if (binding.boundActionNames.isEmpty()) {
            ImGui.beginDisabled();
            ImGui.textWrapped("No Input Action bound to this Controller Binding yet");
            ImGui.endDisabled();
            ImGui.endChild();
            return;
        }

        if (!ImGui.beginTable("##CBE_Bound_Action_List_Layout", 2, ImGuiTableFlags.SizingStretchProp, ImGui.getContentRegionAvail())) {
            ImGui.endChild();
            return;
        }

        ImGui.tableSetupColumn("##CBE_Bound_Action_Name_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("##CBE_Bound_Action_Remove_Column", ImGuiTableColumnFlags.WidthFixed);
        HashSet<String> boundActions = new HashSet<>(binding.boundActionNames);
        for (String name : boundActions) {
            ImGui.tableNextColumn();
            ImGui.textWrapped(name);
            ImGui.tableNextColumn();
            if (EditorWidget.iconButton("Unbound##CBE_Unbound_Input_Action_" + name, EditorIcons.Icons.Delete, "Unbound '" + name + "' Input Action")) {
                binding.boundActionNames.remove(name);
                break;
            }
        }

        ImGui.endTable();
        ImGui.endChild();
    }

    private static void renderInputActionSelectionCombo(ControllerBinding binding) {
        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
        if (!ImGui.beginCombo("##Add_binding_Action_Combo_CBE", "Select a InputAction", ImGuiComboFlags.HeightLargest)) return;
        if (ImGui.beginTable("##CBE_InputAction_Search_Layout", 3, ImGuiTableFlags.SizingStretchProp)) {
            ImGui.tableSetupColumn("##CBE_ActionName_Search_Tile_Column", ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableSetupColumn("##CBE_ActionName_Search_Input_Column", ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableSetupColumn("##CBE_ActionName_Search_Clear_Column", ImGuiTableColumnFlags.WidthFixed);

            ImGui.tableNextColumn();
            ImGui.text("Search:");
            ImGui.tableNextColumn();
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
            ImGui.inputTextWithHint("##CBE_Search_Filter_ActionName", "Enter action name...", actionNameSearchFilter);
            ImGui.tableNextColumn();
            if (EditorWidget.iconButton("Clear##CBE_Search_Clear_ActionName", EditorIcons.Icons.Eraser, "Clear search filter term")) actionNameSearchFilter.clear();
            ImGui.endTable();
        }

        searchActionFilter();
        ImGui.separator();
        if (filteredInputActions.isEmpty()) {
            Map<String, InputAction> inputActions = Project.currentProject().inputActions();
            if (inputActions == null || inputActions.isEmpty()) {
                ImGui.beginDisabled();
                ImGui.textWrapped("Project has no input action. Input action can be added by navigating to Project -> Preferences -> Input Map.");
                ImGui.textWrapped("Or you can click the bellow button to navigate to Input Map now.");
                ImGui.endDisabled();

            } else {
                ImGui.beginDisabled();
                ImGui.textWrapped("There is no Input Action with name contains '" + actionNameSearchFilter.get().trim().toLowerCase() + "'");
                ImGui.endDisabled();
            }

            if (ImGui.button("To Input Map##CBE_Nav_To_InputMap_Button", ImGui.getContentRegionAvailX(), 0.0f)) wantToEditInputMap = true;

            ImGui.endCombo();
            return;
        }

        if (ImGui.beginChild("##CBE_InputAction_Selectable_Region", ImGui.getContentRegionAvailX(), ImGui.getTextLineHeightWithSpacing() * 6.0f, true)) {
            for (Map.Entry<String, InputAction> entry : filteredInputActions.entrySet()) {
                String name = entry.getKey();
                String label = name + "##CBE_Select_InputAction_" + name + "_Selectable";
                if (ImGui.selectable(label)) binding.boundActionNames.add(name);
            }
        }

        ImGui.endChild();

        if (ImGui.button("Edit Input Map##CBE_Nav_To_InputMap_Button", ImGui.getContentRegionAvailX(), 0.0f)) wantToEditInputMap = true;

        ImGui.endCombo();
    }

    private static void searchActionFilter() {
        filteredInputActions.clear();
        Map<String, InputAction> inputActions = Project.currentProject().inputActions();
        if (inputActions == null || inputActions.isEmpty()) return;

        String filterTerm = actionNameSearchFilter.get().trim().toLowerCase();
        if (filterTerm.isEmpty()) {
            filteredInputActions.putAll(inputActions);
            return;
        }

        filteredInputActions.putAll(inputActions.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().contains(filterTerm))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    static void clearDialogData() {
        wantToEditInputMap = false;
        editingName = null;
        editingNameBuffer.clear();
        editingController = null;
        selectedName = null;
        actionNameSearchFilter.clear();
        filteredInputActions.clear();
    }
}

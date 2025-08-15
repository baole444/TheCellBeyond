package editor;

import TheCellBeyond.GameObject;
import TheCellBeyond.Window;
import components.Component;
import components.SpriteRenderer;
import components.TextRenderer;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import scene.Scene;

public class AddComponentDialog {
    private static final String POPUP_ID = "Add New Component";
    private static final String COMPONENT_LIST_ID = "Component_Type_List";
    private static final String DESCRIPTION_SECTION_ID = "Description_section";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(600.0f, 600.0f);
    private static boolean showDialog = false;

    private static GameObject selectedObject = null;
    private static ComponentType selectedType = null;

    private static final float listYPercentage = 0.55f;
    private static final float descriptionYPercentage = 0.2f;
    private static final boolean enableBorder = true;

    // TODO: Need to come up with better solution in the future
    //  to be able to register potential user's custom component type.
    private enum ComponentType {
        SpriteRenderer("SpriteRenderer", "Allow addition of a sprite for rendering to an object."),
        TextRenderer("TextRenderer", "Allow addition of texts for rendering to an object");

        private final String displayLabel;
        private final String description;

        ComponentType(String displayLabel, String description) {
            this.displayLabel = displayLabel;
            this.description = description;
        }

        String label() {
            return displayLabel;
        }

        String description() {
            return description;
        }
    }

    public static void show(GameObject selected) {
        showDialog = true;
        selectedObject = selected;
    }

    public static void imgui() {
        if (!showDialog) return;

        ImGui.openPopup(POPUP_ID);

        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;

        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DIALOG_SIZE, ImGuiCond.FirstUseEver);

        if (ImGui.beginPopupModal(POPUP_ID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.text("Select one component type:");

            ImGui.separator();
            int sectionY = (int) (DIALOG_SIZE.y * listYPercentage);
            ImGui.beginChild(COMPONENT_LIST_ID, ImGuiWindowFlags.None, sectionY, enableBorder);
            for (ComponentType type : ComponentType.values()) {
                boolean isSelected = selectedType == type;

                if (ImGui.selectable(type.label() + "##" + type.name(), isSelected)) {
                    selectedType = type;
                }
            }
            ImGui.endChild();

            ImGui.separator();
            ImGui.text("Description:");
            sectionY = (int) (DIALOG_SIZE.y * descriptionYPercentage);
            ImGui.beginChild(DESCRIPTION_SECTION_ID, ImGuiWindowFlags.None, sectionY, enableBorder);
            if (selectedType != null) {
                ImGui.textWrapped(selectedType.description());
            } else {
                ImGui.textDisabled("Select a component type to see it's description.");
            }
            ImGui.endChild();

            ImGui.separator();
            int buttonW = 120;
            if (selectedType != null) {
                if (ImGui.button("Add", buttonW, 30)) addComponent(selectedType);
            } else {
                ImGui.beginDisabled();
                ImGui.button("Add", buttonW, 30);
                ImGui.endDisabled();
            }

            ImGui.sameLine();

            if (ImGui.button("Cancel", buttonW, 30)) {
                showDialog = false;
                selectedType = null;
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }

        if (!ImGui.isPopupOpen(POPUP_ID)) {
            showDialog = false;
            selectedType = null;
        }
    }

    private static void addComponent(ComponentType type) {
        Scene scene = Window.getScene();
        if (scene == null || selectedObject == null) return;

        Component c;
        switch (type) {
            case SpriteRenderer -> c = new SpriteRenderer();
            case TextRenderer -> c = new TextRenderer();
            default -> c = null;
        }

        if (c != null) selectedObject.addComponent(c);

        showDialog = false;
        selectedType = null;
        ImGui.closeCurrentPopup();
    }
}


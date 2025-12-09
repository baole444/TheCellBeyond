package editor.dialog;

import TheCellBeyond.GameObject;
import TheCellBeyond.Window;
import components.*;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import physic2d.RigidBody2D;
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
        Sprite("Sprite", "Allow addition of a sprite for rendering to the object."),
        AnimatedSprite("AnimatedSprite", "Allow addition of sprite-based animations for rendering to the object."),
        Text("Text", "Allow addition of texts for rendering to an object"),
        TileMap("TileMap", "Allow addition of a grid map that facilitate a tile set for rendering to the object.");

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
        ImGui.setNextWindowSize(DIALOG_SIZE);

        if (ImGui.beginPopupModal(POPUP_ID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.text("Select one component type:");
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

            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - ImGui.getStyle().getWindowPaddingY());

            float buttonWidth = 120;
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float addX = (availX * 0.25f) - (buttonPivotX);
            float cancelX = (availX * 0.75f) - (buttonPivotX);
            ImGui.setCursorPosX(addX);
            if (selectedType != null) {
                if (ImGui.button("Add", buttonWidth, 0)) addComponent(selectedType);
            } else {
                ImGui.beginDisabled();
                ImGui.button("Add", buttonWidth, 0);
                ImGui.endDisabled();
            }

            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, 0)) {
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
            case Sprite -> c = new SpriteRenderer();
            case AnimatedSprite -> c = new AnimatedSpriteRenderer();
            case Text -> c = new TextRenderer();
            case TileMap -> c = new TileMap();
            default -> c = null;
        }

        if (c != null) selectedObject.addComponent(c);

        showDialog = false;
        selectedType = null;
        ImGui.closeCurrentPopup();
    }
}


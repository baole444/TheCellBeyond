package editor.dialog;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.Window;
import editor.Properties;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import scene.Scene;

public class AddObjectDialog {
    private static final String POPUP_ID = "Add New Object";
    private static final String OBJECT_LIST_ID = "Object_Type_List";
    private static final String DESCRIPTION_SECTION_ID = "Description_section";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(600.0f, 600.0f);
    private static boolean showDialog = false;

    private static GameObject parentObject = null;
    private static ObjectType selectedType = null;

    private static final float listYPercentage = 0.6f;
    private static final float descriptionYPercentage = 0.2f;
    private static final boolean enableBorder = true;

    // TODO: Need to come up with better solution in the future
    //  to be able to register potential user's custom object type.
    private enum ObjectType {
        GameObject("GameObject", "The base class of other game object types."),
        GameObject2D("GameObject2D", "The base class of 2D game object types with spatial support.");

        private final String displayLabel;
        private final String description;

        ObjectType(String displayLabel, String description) {
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

    public static void show(GameObject parent) {
        showDialog = true;
        parentObject = parent;
    }

    public static void imgui() {
        if (!showDialog) return;

        ImGui.openPopup(POPUP_ID);

        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;

        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DIALOG_SIZE, ImGuiCond.FirstUseEver);

        if (ImGui.beginPopupModal(POPUP_ID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.text("Select one object type:");
            int sectionY = (int) (DIALOG_SIZE.y * listYPercentage);
            ImGui.beginChild(OBJECT_LIST_ID, ImGuiWindowFlags.None, sectionY, enableBorder);
            for (ObjectType type : ObjectType.values()) {
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
                ImGui.textDisabled("Select an object type to see it's description.");
            }
            ImGui.endChild();
            ImGui.separator();

            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - ImGui.getStyle().getWindowPaddingY());

            float buttonWidth = 120;
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float createX = (availX * 0.25f) - (buttonPivotX);
            float cancelX = (availX * 0.75f) - (buttonPivotX);
            ImGui.setCursorPosX(createX);
            if (selectedType != null) {
                if (ImGui.button("Create", buttonWidth, 0)) createObject(selectedType);
            } else {
                ImGui.beginDisabled();
                ImGui.button("Create", buttonWidth, 0);
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

    private static void createObject(ObjectType type) {
        Scene scene = Window.getScene();
        if (scene == null) return;

        GameObject newObject;
        switch (type) {
            case GameObject2D -> newObject = new GameObject2D(type.label());
            case GameObject -> newObject = new GameObject(type.label());
            default -> newObject = null;
        }

        scene.queueForObjectAddition(newObject, parentObject);
        Properties.setActiveGameObject(newObject);

        showDialog = false;
        selectedType = null;
        ImGui.closeCurrentPopup();
    }
}


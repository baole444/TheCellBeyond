package editor.dialog;

import TheCellBeyond.GameObject;
import TheCellBeyond.internal.LogicServer;
import editor.Properties;
import editor.enums.ObjectType;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import scene.Scene;

/**
 * Editor dialogue for adding game object to scene.
 */
public final class AddObjectDialog {
    private static final String PopupID = "Add New Object";
    private static final String ObjectListID = "Object_Type_List";
    private static final String DescriptionSectionID = "Description_section";
    private static final ImVec2 DialogSize = new ImVec2(600.0f, 600.0f);
    private static boolean showDialog = false;
    private static GameObject parentObject = null;
    private static ObjectType selectedType = null;
    private static final float listYPercentage = 0.6f;
    private static final float descriptionYPercentage = 0.2f;
    private static final boolean enableBorder = true;

    private AddObjectDialog() {}

    /**
     * Toggle the show flag for this dialogue.
     * @param parent the optional parent object for the new game object
     */
    public static void show(GameObject parent) {
        showDialog = true;
        parentObject = parent;
    }

    /**
     * Render the dialogue on screen.
     */
    public static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.text("Select one object type:");
            int sectionY = (int) (DialogSize.y * listYPercentage);
            ImGui.beginChild(ObjectListID, ImGuiWindowFlags.None, sectionY, enableBorder);
            for (ObjectType type : ObjectType.values()) {
                boolean isSelected = selectedType == type;
                if (ImGui.selectable(type.label + "##" + type.name(), isSelected)) {
                    selectedType = type;
                }
            }
            ImGui.endChild();
            ImGui.separator();
            ImGui.text("Description:");
            sectionY = (int) (DialogSize.y * descriptionYPercentage);
            ImGui.beginChild(DescriptionSectionID, ImGuiWindowFlags.None, sectionY, enableBorder);
            if (selectedType != null) {
                ImGui.textWrapped(selectedType.description);
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
        if (!ImGui.isPopupOpen(PopupID)) {
            showDialog = false;
            selectedType = null;
        }
    }

    private static void createObject(ObjectType type) {
        Scene scene = LogicServer.currentScene();
        if (scene == null) return;
        GameObject newObject = ObjectType.getObjectFromType(type);
        scene.queueForObjectAddition(newObject, parentObject);
        Properties.setActiveGameObject(newObject);
        showDialog = false;
        selectedType = null;
        ImGui.closeCurrentPopup();
    }
}


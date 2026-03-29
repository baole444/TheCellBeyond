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
import scripting.ScriptLoader;
import scripting.TypeEntry;
import utility.log.EngineLog;

import java.util.List;

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
    private static TypeEntry selectedCustomType = null;
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
                    selectedCustomType = null;
                }
            }
            List<TypeEntry> customTypes = ScriptLoader.gameObjectTypes();
            if (!customTypes.isEmpty()) {
                ImGui.separator();
                for (TypeEntry entry : customTypes) {
                    boolean isSelected = selectedCustomType == entry;
                    if (ImGui.selectable(entry.label() + "##Custom_" + entry.targetClass().getName(), isSelected)) {
                        selectedCustomType = entry;
                        selectedType = null;
                    }
                }
            }
            ImGui.endChild();
            ImGui.separator();
            ImGui.text("Description:");
            sectionY = (int) (DialogSize.y * descriptionYPercentage);
            ImGui.beginChild(DescriptionSectionID, ImGuiWindowFlags.None, sectionY, enableBorder);
            if (selectedType != null) ImGui.textWrapped(selectedType.description);
            else if (selectedCustomType != null) ImGui.textWrapped(selectedCustomType.description());
            else ImGui.textDisabled("Select an object type to see it's description.");
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
            boolean canCreate = selectedType != null || selectedCustomType != null;
            if (!canCreate) ImGui.beginDisabled();
            if (ImGui.button("Create", buttonWidth, 0.0f)) {
                if (selectedType != null) createObject(selectedType);
                else if (selectedCustomType != null) createCustomObject(selectedCustomType);
            }
            if (!canCreate) ImGui.endDisabled();
            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, 0)) {
                closeDialog();
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }
        if (!ImGui.isPopupOpen(PopupID)) closeDialog();
    }

    private static void createObject(ObjectType type) {
        Scene scene = LogicServer.currentScene();
        if (scene == null) return;
        GameObject newObject = ObjectType.getObjectFromType(type);
        scene.queueForObjectAddition(newObject, parentObject);
        Properties.setActiveGameObject(newObject);
        closeDialog();
        ImGui.closeCurrentPopup();
    }

    private static void createCustomObject(TypeEntry entry) {
        Scene scene = LogicServer.currentScene();
        try {
            GameObject newObject;
            try {
                newObject = (GameObject) entry.targetClass().getConstructor(String.class).newInstance(entry.label());
            } catch (NoSuchMethodException e) {
                newObject = (GameObject) entry.targetClass().getConstructor().newInstance();
                newObject.name(entry.label());
            }
            scene.queueForObjectAddition(newObject, parentObject);
            Properties.setActiveGameObject(newObject);
        } catch (Exception e) {
            EngineLog.error("Scripting", String.format("Failed to create custom object '%s': %s", entry.label(), e.getMessage()));
        }
        closeDialog();
        ImGui.closeCurrentPopup();
    }

    private static void closeDialog() {
        showDialog = false;
        selectedType = null;
        selectedCustomType = null;
    }
}


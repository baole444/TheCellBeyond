package editor.dialog;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.internal.LogicServer;
import TheCellBeyond.TileMap;
import editor.Properties;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import physic2d.CharacterBody2D;
import physic2d.RigidBody2D;
import physic2d.StaticBody2D;
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
        GameObject("GameObject", "A plain game object, the base of other object types. " +
                "It support data serialization, object hierarchy tree and mounting components."),

        GameObject2D("GameObject2D", "A 2D game object, the base of all 2D-related object types. " +
                "It exists in the logic spatial world and can supports transformation."),

        StaticBody2D("StaticBody2D", "A 2D static physic object. It exists in both logic spatial and physic world. The object is immovable."),

        RigidBody2D("RigidBody2D", "A 2D rigid physic object with full physic simulation. " +
                "It exists in both logic spatial and physic world. The transformation of the object is the result of physic simulation via applied forces."),

        CharacterBody2D("CharacterBody2D", "A specialized 2D physic object that is not affected by physics at all, but it affects other physic objects in its path. " +
                "It is used to provide API to move objects in a specific way, as is often the case with user-controlled characters or logic driven NPCs."),

        TileMap("TileMap", "A 2D tile map object. Tile map can have static or kinematic physic body and physic collision defined by tiles in the map's tile set.");

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
        ImGui.setNextWindowSize(DIALOG_SIZE);

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
        Scene scene = LogicServer.currentScene();
        if (scene == null) return;

        GameObject newObject;
        switch (type) {
            case GameObject2D -> newObject = new GameObject2D(type.label());
            case GameObject -> newObject = new GameObject(type.label());
            case StaticBody2D -> newObject = new StaticBody2D(type.label());
            case RigidBody2D -> newObject = new RigidBody2D(type.label());
            case CharacterBody2D -> newObject = new CharacterBody2D(type.label());
            case TileMap -> newObject = new TileMap(type.label());

            default -> newObject = null;
        }

        scene.queueForObjectAddition(newObject, parentObject);
        Properties.setActiveGameObject(newObject);

        showDialog = false;
        selectedType = null;
        ImGui.closeCurrentPopup();
    }
}


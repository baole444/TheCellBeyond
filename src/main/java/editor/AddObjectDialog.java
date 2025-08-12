package editor;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.Window;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import scene.Scene;

public class AddObjectDialog {
    private static final String POPUP_ID = "New_Object_Dialog";
    private static final String OBJECT_LIST_ID = "Object_Type_List";
    private static final String DESCRIPTION_SECTION_ID = "Description_section";
    private static final String BUTTON_SECTION_ID = "Button_Section";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(600.0f, 600.0f);
    private static boolean showDialog = false;

    private static GameObject parentObject = null;
    private static ObjectType selectedType = null;

    private static final float listYPercentage = 0.6f;
    private static final float descriptionYPercentage = 0.25f;
    private static final float buttonYPercentage = 1f - listYPercentage - descriptionYPercentage;
    private static final boolean enableBorder = true;

    // TODO: Need to come up with better solution in the future
    //  to be able to register potential user's custom object type.
    private enum ObjectType {
        GameObject("GameObject", "The base object of other game object types."),
        GameObject2D("GameObject2D", "The base of 2D game object types with spatial support.");

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

        if (ImGui.beginPopupModal("New Object", ImGuiWindowFlags.NoResize)) {
            ImGui.text("Select one object type:");

            ImGui.separator();
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
            ImGui.beginChild(DESCRIPTION_SECTION_ID, ImGuiWindowFlags.None, sectionY, !enableBorder);
            if (selectedType != null) {
                ImGui.textWrapped(selectedType.description());
            } else {
                ImGui.textDisabled("Select an object type to see it's description.");
            }
            ImGui.endChild();

            ImGui.separator();
            sectionY = (int) (DIALOG_SIZE.y * buttonYPercentage);
            int buttonW = 100;
            int buttonH = (int) (sectionY * 0.6f);
            ImGui.beginChild(BUTTON_SECTION_ID, ImGuiWindowFlags.None, sectionY, !enableBorder);
            if (selectedType != null) {
                if (ImGui.button("Create", buttonW, buttonH)) createObject(selectedType);
            } else {
                ImGui.beginDisabled();
                ImGui.button("Create", buttonW, buttonH);
                ImGui.endDisabled();
            }

            ImGui.sameLine();

            if (ImGui.button("Cancel", buttonW, buttonH)) {
                showDialog = false;
                selectedType = null;
                ImGui.closeCurrentPopup();
            }
            ImGui.endChild();

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
        Window.getImGuiLayer().loadProperties().setActiveGameObject(newObject);

        showDialog = false;
        selectedType = null;
        ImGui.closeCurrentPopup();
    }
}


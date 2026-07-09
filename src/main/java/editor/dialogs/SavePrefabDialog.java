package editor.dialogs;

import TheCellBeyond.GameObject;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import utility.prefabrication.PrefabManager;

public final class SavePrefabDialog {
    private static final String PopupID = "Save Prefab";
    private static final ImVec2 DialogSize = new ImVec2(400.0f, 160.0f);
    private static final float ButtonReserver = ImGui.getFrameHeightWithSpacing();
    private static final float ButtonWidth = 100.0f;
    private static final float ButtonHeight = 30.0f;
    private static boolean showDialog = false;
    private static boolean nameTaken = false;
    private static final ImString prefabName = new ImString(128);
    private static String errorMessage = "";
    private static GameObject pendingGO = null;
    private static boolean pendingWithChildren = false;

    private SavePrefabDialog() {}

    public static void show(GameObject go, boolean withChildren) {
        pendingGO = go;
        pendingWithChildren = withChildren;
        prefabName.set(go.name().replaceAll("[^a-zA-z0-9_-]", "_"));
        nameTaken = false;
        errorMessage = "";
        showDialog = true;
    }

    public static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.spacing();
            ImGui.text("Prefab name:");
            ImGui.spacing();
            ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
            if (ImGui.inputTextWithHint("##SPD_Save_Prefab_Input", "Enter a new name...", prefabName)) checkName();
            ImGui.popItemWidth();
            if (nameTaken || !errorMessage.isEmpty()) {
                ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.2f, 0.2f, 1.0f);
                ImGui.textWrapped(errorMessage);
                ImGui.popStyleColor(1);
            } else ImGui.text("    ");
            ImGui.setCursorPosY(ImGui.getWindowHeight() - ButtonReserver - ButtonHeight / 2.0f);
            float startX = ImGui.getCursorStartPosX();
            float buttonPivotX = ButtonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float createX = startX + availX * 0.25f - buttonPivotX;
            float cancelX = startX + availX * 0.75f - buttonPivotX;
            boolean canRename = !prefabName.isEmpty() && !nameTaken && errorMessage.isEmpty();
            ImGui.setCursorPosX(createX);
            if (!canRename) ImGui.beginDisabled();
            if (ImGui.button("Save##SavePrefabConfirm", ButtonWidth, ButtonHeight)) save();
            if (!canRename) ImGui.endDisabled();
            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel##SavePrefabCancel", ButtonWidth, ButtonHeight)) {
                showDialog = false;
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }
        if (!ImGui.isPopupOpen(PopupID)) {
            showDialog = false;
            resetDialogData();
        }
    }

    private static void checkName() {
        String name = prefabName.get().trim();
        if (name.isEmpty()) {
            nameTaken = false;
            errorMessage = "Name cannot be blank";
            return;
        }
        if (PrefabManager.invalidPrefabName(name)) {
            nameTaken = false;
            errorMessage = "Name can only contain letters, numbers, _ and -";
            return;
        }
        if (PrefabManager.prefabNameTaken(name)) {
            nameTaken = true;
            errorMessage = "Prefab '" + name + "' already exists";
            return;
        }
        nameTaken = false;
        errorMessage = "";
    }

    private static void save() {
        String name = prefabName.get().trim();
        if (name.isEmpty() || nameTaken || !errorMessage.isEmpty()) return;
        PrefabManager.savePrefab(pendingGO, name, pendingWithChildren);
        showDialog = false;
        ImGui.closeCurrentPopup();
    }

    private static void resetDialogData() {
        nameTaken = false;
        prefabName.clear();
        errorMessage = "";
        pendingGO = null;
    }
}

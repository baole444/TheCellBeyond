package editor.dialog;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Vector2i;
import utility.IdPool;

import java.nio.file.Path;

public class NewProjectDialog {
    private static final IdPool ID_POOL = new IdPool(0, false);
    private static final String POPUP_ID = "New project";
    private static final String FILE_SELECTION_ID = "File_Selection";
    private static final String PREFERENCE_ID = "Preference_Editor";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(720.0f, 500.0f);
    private static boolean showDialog = false;
    private static final boolean enableBorder = true;

    private static final ImString selectedDirectoryPath = new ImString(256);

    private static final ImString gameTitle = new ImString(128);
    private static final Vector2i gameWindowSize = new Vector2i(640, 480);
    private static boolean allowResize = false;
    private static final ImBoolean resizable = new ImBoolean(false);
    private static boolean maintainAspectRatio = true;
    private static final ImBoolean lockAR = new ImBoolean(true);

    private static final float fileYPercentage = 0.1f;
    private static final float metaYPercentage = 0.8f;

    public static void show() {
        showDialog = true;
        resetDialogData();
    }

    private static void resetDialogData() {
        selectedDirectoryPath.clear();
        gameTitle.clear();
        gameWindowSize.set(640, 480);
        allowResize = false;
        maintainAspectRatio = true;
    }

    public static void imgui() {
        if (!showDialog) return;

        ImGui.openPopup(POPUP_ID);

        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;

        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DIALOG_SIZE, ImGuiCond.FirstUseEver);

        if (ImGui.beginPopupModal(POPUP_ID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            renderDirectorySelection();
            ImGui.text("Preferences");
            renderPreferenceEditor();

            float buttonWidth = 150;
            float buttonHeight = 30;
            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - (buttonHeight / 2) - ImGui.getStyle().getWindowPaddingY());
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float addX = (availX * 0.25f) - (buttonPivotX);
            float cancelX = (availX * 0.75f) - (buttonPivotX);
            boolean canAdd = gameTitle.isNotEmpty() && selectedDirectoryPath.isNotEmpty() && gameWindowSize.x > 0 && gameWindowSize.y > 0;
            ImGui.setCursorPosX(addX);
            if (canAdd) {
                if (ImGui.button("Create project", buttonWidth, buttonHeight)) createProject();
            } else {
                ImGui.beginDisabled();
                ImGui.button("Create project", buttonWidth, buttonHeight);
                ImGui.endDisabled();
            }

            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, buttonHeight)) {
                showDialog = false;
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
            ID_POOL.reset();
        }

        if (!ImGui.isPopupOpen(POPUP_ID)) {
            showDialog = false;
            resetDialogData();
        }
    }

    private static void renderDirectorySelection() {
        ImGui.textWrapped("Click \"Select Directory\" to choose the root directory of the project");

        int sectionY = (int) (ImGui.getContentRegionAvailY() * fileYPercentage);
        ImGui.beginChild(FILE_SELECTION_ID, 0, sectionY, !enableBorder);

        int selectButtonW = 160;
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX() - selectButtonW - ImGui.getStyle().getItemSpacingX());
        ImGui.inputText("##folderpath", selectedDirectoryPath, ImGuiInputTextFlags.ReadOnly);
        ImGui.popItemWidth();

        ImGui.sameLine();
        if (ImGui.button("Select Directory", selectButtonW, 0)) {
            Path openedPath = OpenNoneProjectFolderDialog.openFolderDialog();
            if (openedPath != null) selectedDirectoryPath.set(openedPath.toString());
        }

        ImGui.endChild();
    }

    private static void renderPreferenceEditor() {
        int sectionY = (int) (ImGui.getContentRegionAvailY() * metaYPercentage);
        ImGui.beginChild(PREFERENCE_ID, 0, sectionY, enableBorder);
        ImGui.text("Title:");
        ImGui.inputTextWithHint("##Game title", "Enter a name for the project...", gameTitle);
        if (gameTitle.isEmpty()) {
            ImGui.textColored(ImGui.colorConvertFloat4ToU32(1.0f, 0.2f, 0.2f, 1.0f), "Game title cannot be empty");
        } else {
            ImGui.newLine();
        }

        ImGui.spacing();
        ImGui.text("Window size:");
        ImGui.beginDisabled();
        ImGui.textWrapped("Determine the default size of game window");
        ImGui.endDisabled();
        gameWindowSize.x = inputInt("Width", gameWindowSize.x, 1);
        gameWindowSize.y = inputInt("Height", gameWindowSize.y, 1);
        ImGui.spacing();

        allowResize = ImGui.checkbox("Resizable", resizable);
        ImGui.beginDisabled();
        ImGui.textWrapped("(allow player to resize the game window)");
        ImGui.endDisabled();
        ImGui.spacing();

        maintainAspectRatio = ImGui.checkbox("Lock aspect ratio", lockAR);
        ImGui.beginDisabled();
        ImGui.textWrapped("(maintain the game's intended aspect ratio when window is resized)");
        ImGui.endDisabled();

        ImGui.endChild();
    }

    private static void createProject() {
        System.out.println("Coming soon");
    }

    private static int inputInt(String label, int target, int minValue) {
        String id = label + "_" + ID_POOL.newId();
        ImGui.pushID(id);
        final boolean modified;
        final ImInt destination = new ImInt(target);

        modified = ImGui.inputInt(label, destination);

        if (modified) target = Math.max(destination.get(), minValue);

        ImGui.popID();
        return target;
    }
}

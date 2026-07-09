package editor.dialogs;

import editor.EditorColors;
import editor.preference.RecentProject;
import editor.preference.UserPreference;
import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Vector2i;
import project.ClearColor;
import project.Project;
import project.ProjectPreference;
import project.RenderingSetting;
import utility.IdPool;

import java.nio.file.Path;

public class NewProjectDialog {
    private static final IdPool IDPool = new IdPool(0, false);
    private static final String PopupID = "Create new game project##TCB_Create_New_Game_Project_Dialog";
    private static final String PreferenceID = "Preference_Editor";
    private static final ImVec2 DialogSize = new ImVec2(720.0f, 500.0f);
    private static final float ButtonWidth = 150.0f;
    private static final float ButtonHeight = 30.0f;
    private static final float ButtonReserve = ImGui.getFrameHeightWithSpacing();
    private static boolean showDialog = false;
    private static final boolean enableBorder = true;
    private static final ImString selectedDirectoryPath = new ImString(256);
    private static final ImBoolean projectAlreadyExist = new ImBoolean(false);
    private static final ImString gameTitle = new ImString(128);
    private static final Vector2i gameWindowSize = new Vector2i(640, 480);
    private static final ImBoolean allowResize = new ImBoolean(false);
    private static final ImBoolean maintainAspectRatio = new ImBoolean(true);
    private static final ImFloat globalTextureScale = new ImFloat(1.0f);

    public static void show() {
        showDialog = true;
        resetDialogData();
    }

    private static void resetDialogData() {
        selectedDirectoryPath.clear();
        projectAlreadyExist.set(false);
        gameTitle.clear();
        gameWindowSize.set(640, 480);
        allowResize.set(false);
        maintainAspectRatio.set(true);
        globalTextureScale.set(1.0f);
    }

    public static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(PopupID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DialogSize);
        if (ImGui.beginPopupModal(PopupID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            renderDirectorySelection();
            ImGui.text("Preferences");
            renderPreferenceEditor();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - ButtonReserve - ButtonHeight / 2.0f);
            float startX = ImGui.getCursorStartPosX();
            float buttonPivotX = ButtonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float addX = startX + availX * 0.25f - buttonPivotX;
            float cancelX = startX + availX * 0.75f - buttonPivotX;
            boolean canAdd = !projectAlreadyExist.get() && gameTitle.isNotEmpty() && selectedDirectoryPath.isNotEmpty() && gameWindowSize.x > 0 && gameWindowSize.y > 0;
            ImGui.setCursorPosX(addX);
            if (!canAdd) ImGui.beginDisabled();
            if (ImGui.button("Create project", ButtonWidth, ButtonHeight)) createProject();
            if (!canAdd) ImGui.endDisabled();
            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", ButtonWidth, ButtonHeight)) {
                showDialog = false;
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
            IDPool.reset();
        }
        if (!ImGui.isPopupOpen(PopupID)) {
            showDialog = false;
            resetDialogData();
        }
    }

    private static void renderDirectorySelection() {
        ImGui.spacing();
        ImGui.textColored(EditorColors.InstructionHighLight, "Choose where to store the project");
        ImGui.separator();
        if (!ImGui.beginTable("##NPD_Root_Directory_Selection_Layout_Table", 3)) return;
        ImGui.tableSetupColumn("##NPD_Root_Directory_Selection_Label_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##NPD_Root_Directory_Selection_Input_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("##NPD_Root_Directory_Selection_Search_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableNextColumn();
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (ImGui.getFrameHeightWithSpacing() - ImGui.getTextLineHeightWithSpacing()) / 2.0f);
        ImGui.text("Root directory:");
        ImGui.tableNextColumn();
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        ImGui.inputTextWithHint("##NPD_Selected_Project_Root_Directory", "Click \"Select Directory\" to select a path...", selectedDirectoryPath, ImGuiInputTextFlags.ReadOnly);
        ImGui.popItemWidth();
        ImGui.tableNextColumn();
        if (ImGui.button("Select Directory##NPD_Select_Directory", 150.0f, 0.0f)) {
            Path openedPath = OpenDirectoryDialog.openDialog();
            if (openedPath != null) {
                selectedDirectoryPath.set(openedPath.toString());
                checkForExistingProject(openedPath);
            }
        }
        ImGui.tableNextColumn();
        ImGui.tableNextColumn();
        if (selectedDirectoryPath.isEmpty()) {
            ImGui.newLine();
            ImGui.endTable();
            return;
        }
        if (projectAlreadyExist.get()) ImGui.textColored(ImGui.colorConvertFloat4ToU32(1.0f, 0.2f, 0.2f, 1.0f), "There is already a project at selected directory");
        else ImGui.textColored(ImGui.colorConvertFloat4ToU32(0.2f, 1.0f, 0.2f, 1.0f), "Selected directory is valid");
        ImGui.endTable();
    }

    private static void renderPreferenceEditor() {
        float regionHeight = ImGui.getContentRegionAvailY() - ButtonReserve - ButtonHeight;
        ImGui.beginChild(PreferenceID, 0.0f, regionHeight, ImGuiChildFlags.Borders);
        ImGui.text("Title:");
        ImGui.inputTextWithHint("##Game title", "Enter a name for the project...", gameTitle);
        if (gameTitle.isEmpty()) ImGui.textColored(ImGui.colorConvertFloat4ToU32(1.0f, 0.2f, 0.2f, 1.0f), "Game title cannot be empty");
        else ImGui.newLine();
        ImGui.spacing();
        ImGui.text("Window size:");
        ImGui.beginDisabled();
        ImGui.textWrapped("Determine the default size of game window");
        ImGui.endDisabled();
        gameWindowSize.x = inputInt("Width", gameWindowSize.x, 1);
        gameWindowSize.y = inputInt("Height", gameWindowSize.y, 1);
        ImGui.spacing();
        ImGui.checkbox("Resizable", allowResize);
        ImGui.beginDisabled();
        ImGui.textWrapped("(allow player to resize the game window)");
        ImGui.endDisabled();
        ImGui.spacing();
        ImGui.checkbox("Lock aspect ratio", maintainAspectRatio);
        ImGui.beginDisabled();
        ImGui.textWrapped("(maintain the game's intended aspect ratio when window is resized)");
        ImGui.endDisabled();
        ImGui.spacing();
        ImGui.text("Texture :");
        ImGui.beginDisabled();
        ImGui.textWrapped("Settings that affect the appearance of texture, project-wise.");
        ImGui.endDisabled();
        globalTextureScale.set(inputFloat("Global Scaling", globalTextureScale.get(), 0.01f));
        ImGui.endChild();
    }

    private static void createProject() {
        if (projectAlreadyExist.get()) return;
        if (selectedDirectoryPath.isEmpty() || gameTitle.isEmpty()) return;
        if (gameWindowSize.x < 1 || gameWindowSize.y < 1) return;
        ProjectPreference preference = new ProjectPreference(gameTitle.get(),
                gameWindowSize.x, gameWindowSize.y,
                allowResize.get(), maintainAspectRatio.get(),
                globalTextureScale.get(), new ClearColor(), 60,
                new RenderingSetting()
        );
        Path projectRoot = Path.of(selectedDirectoryPath.get());
        if (!Project.createNewProject(projectRoot, preference)) return;
        String toYML = projectRoot.resolve("_project.yml").toString();
        registerRecentProject(preference, toYML);
        EngineEventCallback.emit(toYML, new EditorEvent(EditorEvent.Type.LoadProjectFromDisk));
    }

    private static int inputInt(String label, int target, int minValue) {
        String id = label + "_" + IDPool.newId();
        ImGui.pushID(id);
        final boolean modified;
        final ImInt destination = new ImInt(target);
        modified = ImGui.inputInt(label, destination);
        if (modified) target = Math.max(destination.get(), minValue);
        ImGui.popID();
        return target;
    }

    private static float inputFloat(String label, float target, float minValue) {
        String id = label + "_" + IDPool.newId();
        ImGui.pushID(id);
        final boolean modified;
        final ImFloat destination = new ImFloat(target);
        modified = ImGui.inputFloat(label, destination);
        if (modified) target = Math.max(destination.get(), minValue);
        ImGui.popID();
        return target;
    }

    private static void checkForExistingProject(Path projectRoot) {
        Path projectYAML = projectRoot.resolve("_project.yml");
        projectAlreadyExist.set(projectYAML.toFile().exists());
    }

    private static void registerRecentProject(ProjectPreference preference, String ymlPath) {
        RecentProject project = new RecentProject(preference.name(), ymlPath, null);
        UserPreference.addRecentProject(project);
    }
}

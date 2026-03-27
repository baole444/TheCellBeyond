package editor.dialog;

import editor.preference.RecentProject;
import project.ProjectData;
import project.ProjectPreference;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

public class RemoveMissingProjectDialog {
    private static final String POPUP_ID = "Missing project";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(400.0f, 160.0f);
    private static boolean showDialog = false;

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static Runnable onRemoveCallback = null;
    private static RecentProject selectedProject = null;

    public static void show(Runnable onRemove, RecentProject recentProject) {
        showDialog = true;
        onRemoveCallback = onRemove;
        selectedProject = recentProject;
    }

    public static void imgui() {
        if (!showDialog) return;
        ImGui.openPopup(POPUP_ID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;
        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DIALOG_SIZE);
        if (ImGui.beginPopupModal(POPUP_ID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.spacing();
            String title = "NULL";
            if (selectedProject != null) title = selectedProject.title() != null ? selectedProject.title() : "Unknow title";
            ImGui.textWrapped("Selected project '" + title + "'cannot be found. Remove it from the list?");
            ImGui.spacing();
            float buttonWidth = 120;
            float buttonHeight = 30;
            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - (buttonHeight / 2) - ImGui.getStyle().getWindowPaddingY());
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float removeX = (availX * 0.25f) - buttonPivotX;
            float cancelX = (availX * 0.75f) - buttonPivotX;
            ImGui.setCursorPosX(removeX);
            if (ImGui.button("Remove", buttonWidth, buttonHeight)) {
                if (onRemoveCallback != null) {
                    Runnable remove = onRemoveCallback;
                    remove.run();
                }
                onRemoveCallback = null;
                showDialog = false;
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, buttonHeight)) {
                showDialog = false;
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }
        if (!ImGui.isPopupOpen(POPUP_ID)) {
            showDialog = false;
            onRemoveCallback = null;
        }
    }

    private static ProjectData getFromYaml(String path) {
        ProjectData selectedProject;
        try {
            File projectFile = new File(path);
            selectedProject = YAML_MAPPER.readValue(projectFile, ProjectData.class);
            if (selectedProject != null && selectedProject.project() == null) {
                System.err.println("Project preference is missing, generating new preference...");
                selectedProject = new ProjectData(selectedProject.version(),
                        new ProjectPreference(), selectedProject.assets(),
                        selectedProject.sheets(), selectedProject.scenes(),
                        selectedProject.inputActions(), selectedProject.physicLayers(),
                        selectedProject.scriptScanDirs()
                );
            }
            return selectedProject;
        } catch (JacksonIOException e) {
            System.err.println("Failed to load project file: " + e.getMessage());
            return null;
        }
    }
}
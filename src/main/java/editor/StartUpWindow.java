package editor;

import TheCellBeyond.MouseListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import editor.dialog.NewProjectDialog;
import editor.dialog.OpenProjectDialog;
import editor.preference.RecentProject;
import editor.preference.UserPreference;
import editor.project.Project;
import editor.project.ProjectData;
import editor.project.ProjectPreference;
import eventviewer.EngineEventCallback;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class StartUpWindow{
    private static final String TABLE_ID = "Project Manager";
    private static final ImVec2 IMGUI_WINDOW_SIZE = new ImVec2(960, 720);
    private static final float projectListXPercentage = 0.75f;

    private static final HashMap<UUID, RecentProject> recentProjects = new HashMap<>();
    private static RecentProject selectedProject = null;

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public static void show(long windowPtr, ImGuiLayer imGuiLayer, int width, int height) {
        MouseListener.setStartupMode(true);

        boolean loaded = false;

        recentProjects.clear();
        recentProjects.putAll(UserPreference.recentProjects());

        while (!glfwWindowShouldClose(windowPtr) && !loaded) {
            glfwPollEvents();

            glClearColor(0.1f, 0.1f, 0.1f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            imGuiLayer.getImGuiGlfw().newFrame();
            imGuiLayer.getImGuiGl3().newFrame();
            ImGui.newFrame();

            ImGui.setNextWindowPos(width / 2.0f, height / 2.0f, ImGuiCond.Always, 0.5f, 0.5f);
            ImGui.setNextWindowSize(IMGUI_WINDOW_SIZE);
            ImGui.begin("Welcome to The Cell Beyond Editor", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse);

            ImGui.text("Project Manager");

            renderProjectList();

            loaded = (Project.currentProject() != null && Project.projectRoot() != null);

            ImGui.end();

            ImGui.render();
            imGuiLayer.getImGuiGl3().renderDrawData(ImGui.getDrawData());

            if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                final long backupWindowPtr = glfwGetCurrentContext();
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
                glfwMakeContextCurrent(backupWindowPtr);
            }

            glfwSwapBuffers(windowPtr);
        }

        recentProjects.clear();
        selectedProject = null;
    }

    private static void renderProjectList() {
        float sectionY = ImGui.getContentRegionAvailY() * 0.9f;
        if (!ImGui.beginChild("Project_Section" ,0.0f, sectionY, false)) {
            ImGui.endChild();
            return;
        }

        ImVec2 remainTableSize = ImGui.getContentRegionAvail();

        if (!ImGui.beginTable(TABLE_ID, 2, ImGuiTableFlags.NoBordersInBody | ImGuiTableFlags.SizingStretchProp, remainTableSize)) {
            ImGui.endTable();
            return;
        }

        ImGui.tableSetupColumn("##Project_List_Column", ImGuiTableColumnFlags.WidthFixed, ImGui.getContentRegionAvailX() * projectListXPercentage);
        ImGui.tableSetupColumn("##Button_Control_Column", ImGuiTableColumnFlags.WidthStretch);

        ImGui.tableNextColumn();
        if (ImGui.beginChild("Project_List_Child", 0.0f, 0.0f, true)) {
            renderSelectableList();
            ImGui.endChild();
        }

        ImGui.tableNextColumn();
        if (ImGui.beginChild("Button_Child", 0.0f, 0.0f, false)) {
            float buttonWidth = ImGui.getContentRegionAvailX();
            float buttonHeight = 30;

            if (selectedProject != null && selectedProject.isPresentedAtPath()) {
                if (ImGui.button("Start Edit", buttonWidth, buttonHeight)) {
                    EngineEventCallback.emit(selectedProject.path(), new Event(EventType.PROJECT_LOAD));
                }
            } else {
                ImGui.beginDisabled();
                ImGui.button("Start Edit", buttonWidth, buttonHeight);
                ImGui.endDisabled();
            }

            ImGui.spacing();

            if (ImGui.button("Open...", buttonWidth, buttonHeight)) {
                Path selectedPath = OpenProjectDialog.openProjectDialog();

                // Check and add if not already existed in the recent project list
                if (selectedPath != null) {
                    boolean inList = false;
                    for (RecentProject project : recentProjects.values()) {
                        if (!project.path().equals(selectedPath.toString())) continue;
                        inList = true;
                        break;
                    }

                    if (!inList) {
                        ProjectData data = getFromYaml(selectedPath.toString());
                        if (data != null) {
                            RecentProject recentProject = new RecentProject(data.project().name(), selectedPath.toString(), null);
                            UserPreference.addRecentProject(recentProject);
                        }
                    }

                    EngineEventCallback.emit(selectedPath.toString(), new Event(EventType.PROJECT_LOAD));
                }
            }
            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();
            ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.8f, 0.3f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.7f, 0.2f, 1.0f);
            if (ImGui.button("New Project", buttonWidth, buttonHeight)) NewProjectDialog.show();
            ImGui.popStyleColor(3);
            ImGui.endChild();
        }

        ImGui.endTable();
        ImGui.endChild();
        NewProjectDialog.imgui();
    }

    private static void renderSelectableList() {
        if (recentProjects.isEmpty()) {
            ImGui.beginDisabled();
            ImGui.textWrapped("No recent project opened. Click \"Open...\" to open a project.");
            ImGui.endDisabled();
            return;
        }

        for (Map.Entry<UUID, RecentProject> entry : recentProjects.entrySet()) {
            RecentProject recentProject = entry.getValue();
            if (recentProject == null) continue;
            float originalX = ImGui.getCursorPosX();
            boolean selected = selectedProject == recentProject;
            if (ImGui.selectable("##" + recentProject.title(), selected, 0.0f, ImGui.getTextLineHeight() * 4.8f)) {
                selectedProject = recentProject;
            }

            ImGui.sameLine();
            ImVec2 cursorPos = ImGui.getCursorPos();
            ImGui.setCursorPos(cursorPos.x, cursorPos.y + ImGui.getTextLineHeight() * 0.3f);
            ImGui.text(recentProject.title());
            ImGui.setCursorPos(cursorPos.x, cursorPos.y + ImGui.getTextLineHeight() * 2.4f);
            ImGui.beginDisabled();
            ImGui.textWrapped(recentProject.path());
            ImGui.endDisabled();
            ImGui.setCursorPosX(cursorPos.x);
            if (!recentProject.isPresentedAtPath()) {
                ImGui.textColored(ImGui.colorConvertFloat4ToU32(1.0f, 0.2f, 0.2f, 1.0f), "Cannot locate project at designated location");
            } else {
                ImGui.newLine();
            }
            float finalY = ImGui.getCursorPosY();
            ImGui.setCursorPos(originalX, finalY + ImGui.getStyle().getWindowPaddingY());
            ImGui.separator();
            ImGui.spacing();
        }
    }

    private static ProjectData getFromYaml(String path) {
        ProjectData selectedProject;
        try {
            File projectFile = new File(path);

            selectedProject = YAML_MAPPER.readValue(projectFile, ProjectData.class);

            if (selectedProject != null) {
                if (selectedProject.project() == null) {
                    System.err.println("Project preference is missing, generating new preference...");
                    selectedProject = new ProjectData(selectedProject.version(),
                            new ProjectPreference(), selectedProject.assets(),
                            selectedProject.sheets(), selectedProject.scenes()
                    );
                }
            }

            return selectedProject;
        } catch (IOException e) {
            System.err.println("Failed to load project file: " + e.getMessage());
            return null;
        }
    }
}

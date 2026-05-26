package editor;

import TheCellBeyond.KeyListener;
import TheCellBeyond.MouseListener;
import TheCellBeyond.Window;
import editor.dialog.NewProjectDialog;
import editor.dialog.OpenProjectDialog;
import editor.dialog.RemoveMissingProjectDialog;
import editor.preference.RecentProject;
import editor.preference.UserPreference;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Callback;
import project.Project;
import project.ProjectData;
import project.ProjectPreference;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import utility.log.Stream2Log;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_Init;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_Quit;

public final class StartupWindow {
    private static final String TABLE_ID = "Project Manager";
    private static final ImVec2 EditorWindowSize = new ImVec2(960, 720);
    private static final float projectListXPercentage = 0.75f;
    private static final HashMap<UUID, RecentProject> recentProjects = new HashMap<>();
    private static RecentProject selectedProject = null;
    private static UUID selectedUUID = null;
    private static String pendingSelection = null;
    private static long windowPtr;
    private static ImGuiLayer imGuiLayer;
    private static final ObjectMapper YAMLMapper = new ObjectMapper(new YAMLFactory()).rebuild().disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();

    public static void init() {
        GLFWErrorCallback.createPrint(Stream2Log.err).set();
        if (!glfwInit()) throw new RuntimeException("Failed to initialize GLFW");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
        glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        int w = (int) EditorWindowSize.x;
        int h = (int) EditorWindowSize.y;
        windowPtr = glfwCreateWindow(w, h, "", NULL, NULL);
        if (windowPtr == NULL) throw new RuntimeException("Failed to create startup window");
        if (!waylandSession()) {
            var screenSize = Window.screenSize();
            glfwSetWindowPos(windowPtr, (screenSize.x - w) / 2, (screenSize.y - h) / 2);
        }
        glfwMakeContextCurrent(windowPtr);
        glfwSwapInterval(1);
        GL.createCapabilities();
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        freeOld(glfwSetCharCallback(windowPtr, KeyListener::charCallback));
        glfwSetInputMode(windowPtr, GLFW_IME, GLFW_TRUE);
        NFD_Init();
        imGuiLayer = new ImGuiLayer(windowPtr);
        imGuiLayer.initImGui("#version 330 core");
        ImGui.getIO().setIniFilename(null);
        glfwShowWindow(windowPtr);
    }

    public static String show() {
        pendingSelection = null;
        recentProjects.clear();
        recentProjects.putAll(UserPreference.recentProjects());
        while (!glfwWindowShouldClose(windowPtr) && pendingSelection == null) {
            glfwPollEvents();
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            imGuiLayer.getImGuiGlfw().newFrame();
            imGuiLayer.getImGuiGl3().newFrame();
            ImGui.newFrame();
            ImGui.setNextWindowPos(EditorWindowSize.x / 2.0f, EditorWindowSize.y / 2.0f, ImGuiCond.Always, 0.5f, 0.5f);
            ImGui.setNextWindowSize(EditorWindowSize);
            if (!ImGui.begin("Welcome to The Cell Beyond Editor", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse)) {
                ImGui.end();
                continue;
            }
            ImGui.text("Project Manager");
            renderProjectList();
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
            if (pendingSelection == null && Project.loaded()) pendingSelection = Project.projectYMLPath();
        }
        recentProjects.clear();
        selectedProject = null;
        selectedUUID = null;
        String result = pendingSelection;
        pendingSelection = null;
        return result;
    }

    public static void dispose() {
        MouseListener.clear();
        KeyListener.clear();
        imGuiLayer.getImGuiGl3().shutdown();
        imGuiLayer.getImGuiGlfw().shutdown();
        ImGui.destroyContext();
        glfwFreeCallbacks(windowPtr);
        glfwDestroyWindow(windowPtr);
        windowPtr = 0;
        imGuiLayer = null;
        glfwTerminate();
        NFD_Quit();
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();
    }

    private static void freeOld(Callback old) {
        if (old != null) old.free();
    }

    private static boolean waylandSession() {
        return glfwGetPlatform() == GLFW_PLATFORM_WAYLAND;
    }

    private static void renderProjectList() {
        float sectionY = ImGui.getContentRegionAvailY() * 0.9f;
        if (!ImGui.beginChild("Project_Section" ,0.0f, sectionY, false)) {
            ImGui.endChild();
            return;
        }
        ImVec2 remainTableSize = ImGui.getContentRegionAvail();
        if (!ImGui.beginTable(TABLE_ID, 2, ImGuiTableFlags.NoBordersInBody | ImGuiTableFlags.SizingStretchProp, remainTableSize)) {
            ImGui.endChild();
            return;
        }
        ImGui.tableSetupColumn("##Project_List_Column", ImGuiTableColumnFlags.WidthFixed, ImGui.getContentRegionAvailX() * projectListXPercentage);
        ImGui.tableSetupColumn("##Button_Control_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableNextColumn();
        renderSelectableList();
        ImGui.tableNextColumn();
        renderButtonRegion();
        ImGui.endTable();
        ImGui.endChild();
        NewProjectDialog.imgui();
        RemoveMissingProjectDialog.imgui();
    }

    private static void renderButtonRegion() {
        if (!ImGui.beginChild("Button_Child", 0.0f, 0.0f, false)) {
            ImGui.endChild();
            return;
        }
        float buttonWidth = ImGui.getContentRegionAvailX();
        float buttonHeight = 30;
        if (selectedProject != null) {
            if (ImGui.button("Start Edit", buttonWidth, buttonHeight)) startEditing();
        } else {
            ImGui.beginDisabled();
            ImGui.button("Start Edit", buttonWidth, buttonHeight);
            ImGui.endDisabled();
        }
        ImGui.spacing();
        if (ImGui.button("Open...", buttonWidth, buttonHeight)) {
            Path selectedPath = OpenProjectDialog.openProjectDialog();
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
                pendingSelection = selectedPath.toString();
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
        float remainHeight = ImGui.getContentRegionAvailY() - buttonHeight;
        ImGui.setCursorPosY(ImGui.getCursorPosY() + remainHeight);
        EditorColors.RedButton.create(() -> {
            if (ImGui.button("Exit...", buttonWidth, buttonHeight)) glfwSetWindowShouldClose(windowPtr, true);
        });
        ImGui.endChild();
    }

    private static void renderSelectableList() {
        if (!ImGui.beginChild("Project_List_Child", 0.0f, 0.0f, true)) {
            ImGui.endChild();
            return;
        }
        if (recentProjects.isEmpty()) {
            ImGui.beginDisabled();
            ImGui.textWrapped("No recent project opened. Click \"Open...\" to open a project.");
            ImGui.endDisabled();
            ImGui.endChild();
            return;
        }
        for (Map.Entry<UUID, RecentProject> entry : recentProjects.entrySet()) {
            UUID key = entry.getKey();
            RecentProject recentProject = entry.getValue();
            if (recentProject == null) continue;
            float originalX = ImGui.getCursorPosX();
            boolean selected = selectedProject == recentProject;
            if (ImGui.selectable("##project_" + key, selected, 0.0f, ImGui.getTextLineHeight() * 4.8f)) {
                selectedUUID = key;
                selectedProject = recentProject;
            }
            if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left)) {
                selectedUUID = key;
                selectedProject = recentProject;
                startEditing();
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
        ImGui.endChild();
    }

    private static ProjectData getFromYaml(String path) {
        ProjectData selectedProject;
        try {
            File projectFile = new File(path);
            selectedProject = YAMLMapper.readValue(projectFile, ProjectData.class);
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

    private static void startEditing() {
        if (selectedProject.isPresentedAtPath()) {
            pendingSelection = selectedProject.path();
            return;
        }
        RemoveMissingProjectDialog.show(() -> {
            if (selectedUUID == null) return;
            UserPreference.removeRecentProject(selectedUUID);
            recentProjects.clear();
            recentProjects.putAll(UserPreference.reloadRecentProject());
            selectedProject = null;
            selectedUUID = null;
        }, selectedProject);
    }
}

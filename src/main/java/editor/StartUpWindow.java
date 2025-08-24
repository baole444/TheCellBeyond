package editor;

import TheCellBeyond.MouseListener;
import editor.dialog.OpenProjectDialog;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiWindowFlags;

import static editor.project.Project.CurrentProject;
import static editor.project.Project.ProjectRoot;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class StartUpWindow{
    public static void show(long windowPtr, ImGuiLayer imGuiLayer, int width, int height) {
        MouseListener.setStartupMode(true);

        boolean loaded = false;

        while (!glfwWindowShouldClose(windowPtr) && !loaded) {
            glfwPollEvents();

            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            imGuiLayer.getImGuiGlfw().newFrame();
            imGuiLayer.getImGuiGl3().newFrame();
            ImGui.newFrame();

            ImGui.setNextWindowPos(width / 2.0f, height / 2.0f, ImGuiCond.Always, 0.5f, 0.5f);
            ImGui.setNextWindowSize(600, 400);
            ImGui.begin("Welcome to The Cell Beyond Editor", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse);

            ImGui.text("Please select a project to open:");

            if (ImGui.button("Open Project", 150, 30)) {
                OpenProjectDialog.openProjectDialog();
            }

            loaded = (CurrentProject != null && ProjectRoot != null);

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
    }
}

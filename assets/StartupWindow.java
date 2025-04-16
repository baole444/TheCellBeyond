package editor;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.util.nfd.NativeFileDialog.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class StartupWindow {
    private long windowPtr;

    private int width = 400;
    private int height = 300;

    private String title = "The Cell Beyond - Project Selection";
    private List<RecentProject> recentProjects = new ArrayList<>();

    // UI state.
    private boolean newProjectHovered = false;
    private boolean openProjectHovered = false;
    private int hoveredProjectIndex = -1;

    // Constants for UI.
    private static final int BUTTON_HEIGHT = 40;
    private static final int BUTTON_WIDTH = 150;
    private static final int PROJECT_ITEM_HEIGHT =60;
    private static final int PADDING = 20;

    // Store recent project information.
    private record RecentProject(String name, String path, long lastOpened) {}

    public void run() {
        System.out.println("Starting LWJGL " + Version.getVersion());

        // Initialize java native file dialog
        if (NFD_Init() != NFD_OKAY) {
            System.err.println("Failed to initialize Native File Dialog: " + NFD_GetError());
        }

        init();
        loadRecentProjects();
        loop();

        // Clean up
        //if (textRenderer != null) {
        //    textRenderer.cleanup();
        //}

        NFD_Quit();

        // Free window callback and destroy the window
        glfwFreeCallbacks(windowPtr);
        glfwDestroyWindow(windowPtr);

        // Terminate GLFW and free the error callback.
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void loadRecentProjects() {
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW.");

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

        // Create the window.
        windowPtr = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowPtr == NULL) throw new RuntimeException("Failed to create GLFW window.");

        //<editor-fold defaultstate="collapsed" desc="Callback setups">
        glfwSetKeyCallback(windowPtr, (windowPtr, key, scancode, action, mod) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) glfwSetWindowShouldClose(windowPtr, true);
        });

        glfwSetMouseButtonCallback(windowPtr, (windowPtr, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_RELEASE) handleMouseClick();
        });

        glfwSetCursorPosCallback(windowPtr, (windowPtr, xPos, yPos) ->{
            updateHoverStates(xPos, yPos);
        });
        //</editor-fold>

        // Get the thread stack and push a new frame
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            // get window's size.
            glfwGetWindowSize(windowPtr, pWidth, pHeight);

            // Get resolution of primary monitor.
            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            if (vidMode != null) {
                glfwSetWindowPos(windowPtr,
                        (vidMode.width() - pWidth.get(0)) / 2,
                        (vidMode.height() - pHeight.get(0)) / 2
                );
            } // stacked frame is popped automatically.

            // Make context current.
            glfwMakeContextCurrent(windowPtr);

            // Vsync.
            glfwSwapInterval(1);

            // Make the window visible.
            glfwShowWindow(windowPtr);

            // Mandatory for externally managed context.
            GL.createCapabilities();

            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        }
    }

    private void handleMouseClick() {

    }

    private void updateHoverStates(double xPos, double yPos) {

    }

    private void loop() {}

}

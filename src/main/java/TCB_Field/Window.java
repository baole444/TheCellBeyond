package TCB_Field;

import components.ImGuiLayer;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private int width, height;
    private String title;
    private long glfwWindow; //windows pointer
    public float r, g, b, a;
    private static Window window = null; // start with no window
    private static Scene currentScene;

    private  String glslVer = null;
    private ImGuiLayer imGuiLayer;

    public Window(ImGuiLayer layer) {
        imGuiLayer = layer;
        this.width = 640;
        this.height = 480;
        this.title = "The Cell Beyond";
        r = 0.027f;
        g = 0.122f;
        b = 0.067f;
        a = 1;
    }

    public static void changeScene(int newScene) {
        switch (newScene)  {
            case 0:
                currentScene = new LevelEditorScene();
                currentScene.init();
                currentScene.start();
                break;
            case 1:
                currentScene = new LevelScene();
                currentScene.init();
                currentScene.start();
                break;
            default:
                assert false: "Unknown scene '" + newScene + "'";
                break;
        }
    }

    public static Window get() {
        // make new windows at begin
        if (Window.window == null) {
            Window.window = new Window(new ImGuiLayer());
        }

        return Window.window;

    }

    public static Scene getScene() {
        return get().currentScene;
    }

    public void run() {
        System.out.println("Starting " + Version.getVersion() + " i");

        init();

        loop();

        //free memories

        endScr();
        //end GLFW and error callback

        glfwSetErrorCallback(null).free();
    }

    public void init() {
        initWindow();
        initImGui();
        imGuiGlfw.init(glfwWindow, true);
        imGuiGl3.init(glslVer);
    }


    private void initWindow() {
        //error return
        GLFWErrorCallback.createPrint(System.err).set();

        //start GLFW
        if (!glfwInit()) {
            System.out.println("Unable to start GLFW.");
            System.exit(-1);
        }

        glslVer = "#version 410";
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);

        //config GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_FALSE);

        //spawn window
        glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
        if (glfwWindow == NULL) {
            System.out.println("Failed to spawn window.");
            System.exit(-1);
        }

        glfwSetCursorPosCallback(glfwWindow, MouseListener::mousePosCallback); // :: is java syntax lambda function
        glfwSetMouseButtonCallback(glfwWindow, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(glfwWindow, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(glfwWindow, KeyListener::keyCallback);
        // OpenGL context current
        glfwMakeContextCurrent(glfwWindow);

        //V-sync yes
        glfwSwapInterval(1);

        //Make window visible
        glfwShowWindow(glfwWindow);

        GL.createCapabilities();
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        Window.changeScene(0);
    }

    public final long getGlfwWindow () {
        return glfwWindow;
    }

    public void initImGui() {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
    }

    public void endScr(){
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
        glfwFreeCallbacks(window.getGlfwWindow());
        glfwDestroyWindow(window.getGlfwWindow());
        glfwTerminate();
    }

    public void loop () {


        float beginTime = (float)glfwGetTime();
        float endTime;
        float dt = -1.0f;

        while (!glfwWindowShouldClose(glfwWindow)) {

            glClearColor(r, g, b, a);
            glClear(GL_COLOR_BUFFER_BIT);

            imGuiGlfw.newFrame();
            ImGui.newFrame();

            imGuiLayer.imgui();

            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                final long backupWindowPtr = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
                GLFW.glfwMakeContextCurrent(backupWindowPtr);
            }


            if (dt >= 0) {
                currentScene.update(dt);
            }


            glfwSwapBuffers(glfwWindow);
            glfwPollEvents(); //poll events
            endTime = (float)glfwGetTime();
            dt = endTime - beginTime;
            beginTime = endTime;
        }

    }
}

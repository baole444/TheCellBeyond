package TCB_Field;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiBackendFlags;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import render.DebugDraw;
import render.FrameBuffer;
import scene.LevelEditorScene;
import scene.LevelScene;
import scene.Scene;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private int width;
    private int height;
    private String title;
    private long glfwWindow; //windows pointer
    public float r, g, b, a;
    private static Window window = null; // start with no window
    private static Scene currentScene;

    private  String glslVer = null;
    private ImGuiLayer imGuiLayer;
    private FrameBuffer frameBuffer;

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
                break;
            case 1:
                currentScene = new LevelScene();
                break;
            default:
                assert false: "Unknown scene '" + newScene + "'";
                break;
        }

        currentScene.loadLevel();
        currentScene.init();
        currentScene.start();
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

    public static int loadWidth() {
        return get().width;
    }

    public static int loadHeight() {
        return get().height;
    }

    public void run() {
        System.out.println("Starting LWJGL " + Version.getVersion() + "!");

        init();

        loop();

        //free memories

        endScr();
        //end GLFW and error callback

        glfwSetErrorCallback(null).free();
    }

    private void init() {
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

        this.frameBuffer = new FrameBuffer(1920, 1080);
        glViewport(0, 0, 1920, 1080);

    }

    public final long getGlfwWindow () {
        return glfwWindow;
    }

    private void initImGui() {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setConfigFlags(ImGuiConfigFlags.NavEnableKeyboard); // Navigation with keyboard
        io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors); // Mouse cursors to display while resizing windows etc.
        imGuiLayer.guiFont(io);
        imGuiLayer.guiMouseCallback(glfwWindow, io);
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        io.setConfigFlags(ImGuiConfigFlags.DockingEnable);

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

            DebugDraw.startFrame();

            this.frameBuffer.use();

            glClearColor(r, g, b, a);
            glClear(GL_COLOR_BUFFER_BIT);



            if (dt >= 0) {
                DebugDraw.draw();
                currentScene.update(dt);
            }
            this.frameBuffer.detach();

            this.imGuiLayer.update(glfwWindow, dt, currentScene, ImGui.getIO(), imGuiGlfw, imGuiGl3);

            glfwSwapBuffers(glfwWindow);
            glfwPollEvents(); //poll events
            endTime = (float)glfwGetTime();
            dt = endTime - beginTime;
            beginTime = endTime;

            currentScene.saveLevel();
        }
    }

    public static FrameBuffer loadFrameBuffer() {
        return get().frameBuffer;
    }

    public static float loadTargetAspectRatio() {
        return 4.0f / 3.0f;
    }
}

package TCB_Field;

import editor.Properties;
import eventviewer.EventSystem;
import eventviewer.EventViewer;
import eventviewer.event.Event;
import imgui.ImGui;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import render.*;
import scene.LevelEditorScene;
import scene.Scene;
import scene.SceneInit;
import utility.AssetsPool;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window implements EventViewer {
    private int width;
    private int height;
    private String title;
    private long glfwWindow; //windows pointer
    public float r, g, b, a;
    private static Window window = null; // start with no window
    private static Scene currentScene;
    private boolean runtimeStart = false;

    private  String glslVer = null;
    private ImGuiLayer imGuiLayer;
    private FrameBuffer frameBuffer;
    private ObjectSelection objectSelection;
    private Properties properties;

    public Window() {
        this.width = 640;
        this.height = 480;
        this.title = "The Cell Beyond";
        r = 0.027f;
        g = 0.122f;
        b = 0.067f;
        a = 1;
        EventSystem.addViewer(this);
    }

    public static void changeScene(SceneInit sceneInit) {
        if (currentScene != null) {
            // End scene
            currentScene.end();

        }
        loadImGui().loadProperties().setActiveObj(null);

        currentScene = new Scene(sceneInit);

        currentScene.loadLevel();
        currentScene.init();
        currentScene.start();
    }

    public static Window get() {
        // make new windows at begin
        if (Window.window == null) {
            Window.window = new Window();
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
        System.out.println("Starting LWJGL " + Version.getVersion());

        initWindow();

        loop();

        //free memories

        endScr();
        //end GLFW and error callback

        glfwSetErrorCallback(null).free();
    }

    private void initWindow() {
        //error return
        GLFWErrorCallback.createPrint(System.err).set();

        //start GLFW
        if (!glfwInit()) {
            System.out.println("Unable to start GLFW.");
            System.exit(-1);
        }

        glslVer = "#version 330 core";
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

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

        this.frameBuffer = new FrameBuffer(1920, 1080);
        this.objectSelection = new ObjectSelection(1920, 1080);

        glViewport(0, 0, 1920, 1080);

        this.imGuiLayer = new ImGuiLayer(glfwWindow, objectSelection);
        this.imGuiLayer.initImGui(glslVer);

        Window.changeScene(new LevelEditorScene());
    }

    public void endScr(){
        imGuiLayer.getImGuiGl3().dispose();
        imGuiLayer.getImGuiGlfw().dispose();
        ImGui.destroyContext();
        glfwFreeCallbacks(window.glfwWindow);
        glfwDestroyWindow(window.glfwWindow);
        glfwTerminate();
    }

    public void loop () {

        float beginTime = (float)glfwGetTime();
        float endTime;
        float dt = -1.0f;

        Shader defaultShader = AssetsPool.loadShader("assets/shaders/default.glsl");
        Shader objectSelectShader = AssetsPool.loadShader("assets/shaders/objSelection.glsl");

        while (!glfwWindowShouldClose(glfwWindow)) {

            // Pass 1: object selection layer (invisible)
            glDisable(GL_BLEND);
            objectSelection.useWrite();

            glViewport(0, 0, 1920, 1080);
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            Renderer.setShader(objectSelectShader);
            currentScene.render();

            objectSelection.detachWrite();
            glEnable(GL_BLEND);

            // Pass 2: Visualized scene

            DebugDraw.startFrame();

            this.frameBuffer.use();

            glClearColor(r, g, b, a);
            glClear(GL_COLOR_BUFFER_BIT);

            if (dt >= 0) {
                DebugDraw.draw();
                Renderer.setShader(defaultShader);

                if (runtimeStart) {
                    currentScene.update(dt);
                } else {
                    currentScene.updateEditor(dt);
                }

                currentScene.render();
            }
            this.frameBuffer.detach();

            this.imGuiLayer.update(dt, currentScene);

            glfwSwapBuffers(glfwWindow);

            MouseListener.endFrame();

            glfwPollEvents(); //poll events
            endTime = (float)glfwGetTime();
            dt = endTime - beginTime;
            beginTime = endTime;
        }
    }

    public static FrameBuffer loadFrameBuffer() {
        return get().frameBuffer;
    }

    public static float loadTargetAspectRatio() {
        return 4.0f / 3.0f;
    }

    public static ImGuiLayer loadImGui() {
        return get().imGuiLayer;
    }

    @Override
    public void whenNotice(GameObject object, Event event) {
        switch (event.type) {
            case EngineStart:
                System.out.println("Start event passed!");
                this.runtimeStart = true;
                currentScene.saveLevel();
                Window.changeScene(new LevelEditorScene());
                break;
            case EngineEnd:
                System.out.println("End event passed!");
                this.runtimeStart = false;
                Window.changeScene(new LevelEditorScene());
                break;
            case LevelLoad:
                Window.changeScene(new LevelEditorScene());
                break;
            case LevelSave:
                currentScene.saveLevel();
                break;

        }
    }
}

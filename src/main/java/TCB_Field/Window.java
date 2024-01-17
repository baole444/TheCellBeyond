package TCB_Field;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import utility.Time;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private int width, height;
    private String title;
    private long glfwWindow;
    public float r, g, b, a;
    private boolean fadeToBlack = false;
    private static Window window = null; // start with no window
    private static Scene currentScene;
    private Window() {
        this.width = 640;
        this.height = 480;
        this.title = "The Cell Beyond";
        r = 1;
        g = 1;
        b = 1;
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
            Window.window = new Window();
        }

        return Window.window;
    }

    public void run() {
        System.out.println("Starting " + Version.getVersion() + " i");

        init();
        loop();

        //free fcking memories

        glfwFreeCallbacks(glfwWindow);
        glfwDestroyWindow(glfwWindow);

        //end GLFW and error callback

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void init() {
        //error return
        GLFWErrorCallback.createPrint(System.err).set();

        //start GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to start GLFW.");
        }

        //config GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_FALSE);

        //spawn window
        glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
        if (glfwWindow == NULL) {
            throw new IllegalStateException("Failed to spawn window.");
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

        /*
        LWJGL's interoperation with GLFW's
        openGL context, or any that managed externally
        LWJGL detect context that is in current thread,
        bindings available for use.
        */
        GL.createCapabilities();

        Window.changeScene(0);
    }

    public void loop () {
        float beginTime = Time.getTime();
        float endTime;
        float dt = -1.0f;

        while (!glfwWindowShouldClose(glfwWindow)) {
            //poll events
            glfwPollEvents();

            glClearColor(r, g, b, a);
            glClear(GL_COLOR_BUFFER_BIT);

            if (dt >= 0) {
                currentScene.update(dt);
            }

            glfwSwapBuffers(glfwWindow);

            endTime = Time.getTime();
            dt = endTime - beginTime;
            beginTime = endTime;
        }

    }
}

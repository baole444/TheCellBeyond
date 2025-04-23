import TCB_Field.*;
import components.TextComponent;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFWErrorCallback;

import org.lwjgl.opengl.GL;
import render.FrameBuffer;

import render.Renderer;
import render.Shader;
import render.text.DirectTextRenderer;
import utility.AssetsPool;
import utility.Settings;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_MAXIMIZED;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;

import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;

import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

public class TestWindow {
    private int width;
    private int height;
    private long glfwWindow; //windows pointer
    private static TestWindow window = null; // start with no window
    public float r, g, b, a;
    private String glslVer = null;
    private FrameBuffer frameBuffer;
    private Viewport viewport = new Viewport(new Vector2f(0 , 0));

    public static void main(String[] args) {
        TestWindow testWindow = TestWindow.get();

        testWindow.run();
    }


    private TestWindow() {
        this.width = 400;
        this.height = 300;

        r = 0.027f;
        g = 0.122f;
        b = 0.067f;
        a = 1;
    }

    private static TestWindow get() {
        if (TestWindow.window == null) {
            TestWindow.window = new TestWindow();
        }

        return TestWindow.window;
    }

    private void run() {
        initWindow();

        loop();

        endScr();

        glfwSetErrorCallback(null).free();
    }

    private void endScr() {
        frameBuffer.dispose();
        glfwFreeCallbacks(glfwWindow);
        glfwDestroyWindow(glfwWindow);
        glfwTerminate();
    }

    private void loop() {
        float beginTime = (float)glfwGetTime();
        float endTime;
        float dt = -1.0f;

        Shader defaultShader = AssetsPool.loadShader(Settings.PATH.DEFAULT_TEXTURE_SHADER);

        DirectTextRenderer textRenderer = DirectTextRenderer.get();

        textRenderer.setProjectionMatrix(viewport.getProjectMatrix());

        textRenderer.setViewMatrix(viewport.getViewMatrix());

        TextComponent test = textRenderer.drawText("Hello world ! TEST 1 2 3", 2f, 1.5f, 24, new Vector4f(0.5f, 1f, 0.7f, 1f));
        test.setVerticalAlignment(TextComponent.VerticalAlignment.MIDDLE);
        test.setHorizontalAlignment(TextComponent.HorizontalAlignment.CENTER);

        while (!glfwWindowShouldClose(glfwWindow)) {
            glfwPollEvents(); //poll events

            if (dt >= 0) {
                Renderer.setShader(defaultShader);

                frameBuffer.captureAndRender(() -> {
                    glClearColor(r, g, b, a);
                    glClear(GL_COLOR_BUFFER_BIT);

                    viewport.adjustProjection();

                    textRenderer.render();
                });
            }

            glfwSwapBuffers(glfwWindow);

            endTime = (float)glfwGetTime();
            dt = endTime - beginTime;
            beginTime = endTime;
        }


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
        //glfwWindowHint(GLFW_DECORATED, 0);

        // Spawn window
        glfwWindow = glfwCreateWindow(this.width, this.height, "Test window", NULL, NULL);
        if (glfwWindow == NULL) {
            System.out.println("Failed to spawn window.");
            System.exit(-1);
        }
        System.out.println("Generating Window, dimension: " + this.width + " x " + this.height);

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

        glViewport(0, 0, this.width, this.height);

    }
}

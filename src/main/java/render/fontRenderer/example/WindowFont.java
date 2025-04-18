package render.fontRenderer.example;

import org.lwjgl.opengl.GL;
import render.FrameBuffer;
import render.Shader;
import render.fontRenderer.FontBatch;
import render.fontRenderer.TCBFont;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.system.MemoryUtil.NULL;

class WindowFont {
    private long windowPtr;
    private TCBFont font;
    private FrameBuffer frameBuffer;

    WindowFont() {
        init();

        try {
            font = new TCBFont("assets/fonts/Consola.ttf", 64, false);
        } catch (IOException e) {
            System.err.println("Failed to generate font, ending test application...");
            e.printStackTrace();
        }
    }

    private void init() {
        glfwInit();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        windowPtr = glfwCreateWindow(1920, 1080, "Example Font Rendering", NULL, NULL);

        if (windowPtr == NULL) {
            System.err.println("Failed to create test window.");
            glfwTerminate();
            return;
        }

        glfwMakeContextCurrent(windowPtr);
        glfwSwapInterval(1);
        glfwShowWindow(windowPtr);

        GL.createCapabilities();

        this.frameBuffer = new FrameBuffer(1410, 900);

    }

    void run() {
        Shader fontShader = new Shader("assets/shaders/defaultFont.glsl");
        Shader sdfShader = new Shader("assets/shaders/defaultSDF.glsl");

        FontBatch batch = new FontBatch().setShader(fontShader).setSdfShader(sdfShader).setFont(font);

        batch.initFontRenderBatch();

        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        while (!glfwWindowShouldClose(windowPtr)) {
            //this.frameBuffer.use();

            glClear(GL_COLOR_BUFFER_BIT);
            glClearColor(0.1f, 0.09f, 0.1f, 1);

            batch.addTextString("Hello world!", 200, 200, 1f, 0xFF00AB0);

            batch.flushBatch();

            //this.frameBuffer.detach();

            glfwSwapBuffers(windowPtr);

            glfwPollEvents();

        }

    }
}

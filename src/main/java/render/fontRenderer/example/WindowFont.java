package render.fontRenderer.example;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL;
import render.Shader;
import render.fontRenderer.CharInfo;
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

    WindowFont() {
        init();

        try {
            font = new TCBFont("assets/fonts/Caudex.ttf", 32, false);
        } catch (IOException e) {
            System.err.println("Failed to generate font, ending test application...");
            e.printStackTrace();
        }
    }

    private void init() {
        glfwInit();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        windowPtr = glfwCreateWindow(1410, 900, "Example Font Rendering", NULL, NULL);

        if (windowPtr == NULL) {
            System.err.println("Failed to create test window.");
            glfwTerminate();
            return;
        }

        glfwMakeContextCurrent(windowPtr);
        glfwSwapInterval(1);
        glfwShowWindow(windowPtr);

        GL.createCapabilities();
    }

    void run() {
        Shader fontShader = new Shader("assets/shaders/defaultFont.glsl");

        FontBatch batch = new FontBatch().setShader(fontShader).setFont(font);

        batch.initFontRenderBatch();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        while (!glfwWindowShouldClose(windowPtr)) {
            glClear(GL_COLOR_BUFFER_BIT);
            glClearColor(0.1f, 0.1f, 0.1f, 1f);

            batch.addTextString("The Cell Beyond", 200, 200, 1f, new Vector3f(0.5f, 0.5f, 0.5f));

            batch.flushBatch();

            glfwSwapBuffers(windowPtr);
            glfwPollEvents();
        }

    }
}

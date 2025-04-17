package render.fontRenderer.cfont;


import org.lwjgl.opengl.GL;
import render.fontRenderer.cfont.Fonts.CFont;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private long window;
    private CFont font;

    public Window() {
        init();
        font = new CFont("C:/Windows/Fonts/Consola.ttf", 64);
    }

    private void init() {
        glfwInit();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(1920, 1080, "Font Rendering", NULL, NULL);
        if (window == NULL) {
            System.out.println("Could not create window.");
            glfwTerminate();
            return;
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        // Initialize gl functions for windows using GLAD
        GL.createCapabilities();
    }

    public void run() {
//        Sdf.generateCodepointBitmap(
//                'G', "C:/Windows/Fonts/arial.ttf", 64);

        Shader fontShader = new Shader("assets/fontShader.glsl");
        Shader sdfShader = new Shader("assets/sdfShader.glsl");
        Batch batch = new Batch();
        batch.shader = fontShader;
        batch.sdfShader = sdfShader;
        batch.font = font;
        batch.initBatch();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT);
            glClearColor(0.1f, 0.09f, 0.1f, 1);

            batch.addText("Hello world!", 200, 200, 1f, 0xFF00AB0);

            //batch.addCharacter(0, 0, 620.0f, oneQuad, 0xFF4500);
            batch.flushBatch();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
}

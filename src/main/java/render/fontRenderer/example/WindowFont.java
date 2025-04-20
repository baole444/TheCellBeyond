package render.fontRenderer.example;

import org.joml.Vector4f;
import org.lwjgl.opengl.GL;
import render.FrameBuffer;
import render.Shader;
import render.fontRenderer.FontBatch;
import render.fontRenderer.FontRenderer;
import render.fontRenderer.TCBFont;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

class WindowFont {
    private long windowPtr;
    private TCBFont font;
    private FrameBuffer frameBuffer;

    WindowFont() {
        init();
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

        FontRenderer.initFontRenderer();

    }

    void run() {
        try {
            // Create and initialize font
            font = new TCBFont("assets/fonts/Consola.ttf", 32, false);
            System.out.println("Font initialized with texture ID: " + font.textureId);

            // Create and compile shader for font rendering
            Shader fontShader = new Shader("assets/shaders/defaultFont.glsl");
            fontShader.compile();

            // Create FontBatchRenderer
            FontBatch textRenderer = new FontBatch("assets/fonts/Consola.ttf", 16, fontShader);

            // Set up ortho projection for 2D rendering
            org.joml.Matrix4f projection = new org.joml.Matrix4f()
                    .ortho(0, 1920, 0, 1080, -1, 10);
            textRenderer.setProjection(projection);

            // Variables for animation and FPS calculation
            float time = 0;
            long lastTime = System.nanoTime();
            double fps = 0;

            // Main loop
            while (!glfwWindowShouldClose(windowPtr)) {
                // Calculate delta time and FPS
                long currentTime = System.nanoTime();
                float dt = (currentTime - lastTime) / 1_000_000_000.0f;
                lastTime = currentTime;
                time += dt;

                fps = 0.95 * fps + 0.05 * (1.0 / dt); // Smooth FPS counter

                // Clear screen
                glClear(GL_COLOR_BUFFER_BIT);
                glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

                /*
                // Add static text
                textRenderer.addText("Font Rendering Demo", 100, 900, 2.0f, 0xFFFFFFFF);

                // Different colors
                textRenderer.addText("Red Text", 100, 800, 1.5f, 0xFF0000FF);
                textRenderer.addText("Green Text", 100, 750, 1.5f, 0x00FF00FF);
                textRenderer.addText("Blue Text", 100, 700, 1.5f, 0x0000FFFF);

                // Dynamic text
                textRenderer.addText(String.format("FPS: %.1f", fps), 100, 600, 1.0f, 0xFFFF00FF);
                textRenderer.addText("Delta Time: " + String.format("%.5f", dt) + " seconds", 100, 550, 1.0f, 0xFFFF00FF);

                // Animated/moving text
                float bounce = (float) Math.sin(time * 2) * 50;
                textRenderer.addText("Bouncing Text", 400, 400 + bounce, 1.5f, 0x00FFFFFF);

                // Color animation
                float r = (float) (Math.sin(time) * 0.5 + 0.5);
                float g = (float) (Math.sin(time + 2) * 0.5 + 0.5);
                float b = (float) (Math.sin(time + 4) * 0.5 + 0.5);
                textRenderer.addText("Rainbow Text", 400, 300, 1.5f,
                        new org.joml.Vector4f(r, g, b, 1.0f));

                // Instructions
                textRenderer.addText("Press ESC to exit", 100, 100, 1.0f, 0xFFFFFFFF);
                 */


                textRenderer.addText("Test", 100, 100, 1, new Vector4f(255, 255, 255, 255));


                // Render all text at once
                textRenderer.render();

                // Handle window events and buffer swap
                glfwSwapBuffers(windowPtr);
                glfwPollEvents();

                // Exit on ESC key
                if (glfwGetKey(windowPtr, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                    glfwSetWindowShouldClose(windowPtr, true);
                }
            }

            // Clean up resources
            textRenderer.dispose();

        } catch (Exception e) {
            System.err.println("Error in run method: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

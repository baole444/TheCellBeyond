package threading;

import editor.ImGuiLayer;
import TheCellBeyond.KeyListener;
import TheCellBeyond.MouseListener;
import TheCellBeyond.Window;
import render.FrameBuffer;
import render.ObjectSelection;
import scene.Scene;

import static org.lwjgl.glfw.GLFW.*;

public class WindowThreadIntegration {
    private Window window;

    private ThreadEngine threadEngine;

    public WindowThreadIntegration(Window window) {
        this.window = window;
    }

    public void initialize() {
        Scene currentScene = Window.getScene();
        if (currentScene == null) {
            throw new IllegalArgumentException("Cannot initialize without a scene.");
        }

        FrameBuffer frameBuffer = Window.getFrameBuffer();
        ObjectSelection objectSelection = window.getObjectSelection();

        threadEngine = new ThreadEngine(currentScene, currentScene.getRenderer(), objectSelection, frameBuffer, window.getWindowPtr());

        threadEngine.initialize();
    }

    /**
     * Window's main loop to handle ImGui updates and GLFW events.
     */
    public void mainLoop() {
        if (threadEngine == null) {
            throw new IllegalStateException("Thread engine not initialized");
        }

        ImGuiLayer imGuiLayer = Window.getImGuiLayer();
        Scene currentScene = Window.getScene();
        long windowPtr = window.getWindowPtr();
        
        while (threadEngine.isRunning() && !glfwWindowShouldClose(windowPtr)) {
            glfwPollEvents();

            imGuiLayer.update(0.016f, currentScene);

            MouseListener.endFrame();
            KeyListener.endFrame();

            glfwSwapBuffers(windowPtr);

            // Avoid hogging CPU by quick sleep
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        threadEngine.stop();
    }

    public ThreadEngine getThreadEngine() {
        return threadEngine;
    }
}

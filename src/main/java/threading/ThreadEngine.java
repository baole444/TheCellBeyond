package threading;

import TheCellBeyond.GameObject;
import TheCellBeyond.Viewport;
import render.*;
import render.text.FontManager;
import render.text.TextBatch;
import scene.Scene;
import threading.states.GameObjectState;
import threading.states.GameState;
import threading.states.SpriteRenderState;
import utility.AssetsPool;
import utility.Settings;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.opengl.GL11.*;

public class ThreadEngine {
    private final TripleBufferManager bufferManager = new TripleBufferManager();
    private volatile boolean running = true;

    private Thread gameLogicThread;
    private Thread renderThread;

    private Scene gameScene;
    private Renderer gameRenderer;
    private ObjectSelection objectSelection;
    private FrameBuffer frameBuffer;
    private long windowPtr;

    private volatile boolean logicThreadReady = false;
    private volatile boolean renderThreadReady = false;

    private Shader defaultShader;
    private Shader objectSelectionShader;

    /**
     * Create a new thread engine
     * @param scene the game scene.
     * @param renderer the renderer.
     * @param objectSelection the object selection layer.
     * @param frameBuffer the frame buffer.
     * @param windowPtr the GLFW window pointer.
     */
    public ThreadEngine(Scene scene, Renderer renderer, ObjectSelection objectSelection, FrameBuffer frameBuffer, long windowPtr) {
        this.gameScene = scene;
        this.gameRenderer = renderer;
        this.objectSelection = objectSelection;
        this.frameBuffer = frameBuffer;
        this.windowPtr = windowPtr;
    }

    /**
     * Initialize and start game logic and render thread.
     */
    public void initialize() {
        // Setup shader in the main thread before creating the render thread
        defaultShader = AssetsPool.loadShader(Settings.PATH.DEFAULT_TEXTURE_SHADER);
        objectSelectionShader = AssetsPool.loadShader(Settings.PATH.OBJECT_SELECTION_SHADER);

        gameLogicThread = new Thread(this::gameLogicLoop, "LogicThread");
        renderThread = new Thread(this::renderLoop, "RenderThread");

        // Set threads as background thread
        gameLogicThread.setDaemon(true);
        renderThread.setDaemon(true);

        // Start threads
        gameLogicThread.start();
        renderThread.start();

        // Wait for threads to be ready
        while (!logicThreadReady || !renderThreadReady) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void gameLogicLoop() {
        long frameNumber = 0;
        float targetDt = 1f / 60f;

        logicThreadReady = true;

        while (running) {
            float beginTime = (float) glfwGetTime();

            // Get write buffer
            GameState writeBuffer = bufferManager.getWriteBuffer();

            // Update game logic
            updateGameLogic(writeBuffer, targetDt);

            // Mark the buffer as ready for rendering
            writeBuffer.setFrameNumber(frameNumber);
            writeBuffer.setDeltaTime(targetDt);
            bufferManager.completeGameLogicUpdate(frameNumber++);

            // Calculate sleep time for fixed timestep
            float endTime = (float) glfwGetTime();
            float sleepTime = targetDt - (endTime - beginTime);
            if (sleepTime > 0) {
                try {
                    Thread.sleep((long) (sleepTime * 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void renderLoop() {
        glfwMakeContextCurrent(windowPtr);

        renderThreadReady = true;

        while (running) {
            GameState renderBuffer = bufferManager.beginRendering();

            if (renderBuffer != null) {
                //renderScene(renderBuffer);
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void updateGameLogic(GameState state, float dt) {
        gameScene.getFlatPhysic().update(dt);

        for (int i = 0; i < gameScene.getGameObjects().size(); i++) {
            GameObject go = gameScene.getGameObjects().get(i);
            go.update(dt);

            if (go.isRemoved()) {
                gameScene.getGameObjects().remove(i);
                gameRenderer.queueObjectForRemoval(go);
                gameScene.getFlatPhysic().destroyObject(go);
                i--;
            }
        }

        // Additional logic update, or user's scripts in the future.

        // Capture current scene state into write buffer
        state.captureFrom(gameScene);
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Stop the engine and its threads
     */
    public void stop() {
        running = false;

        try {
            if (gameLogicThread != null) gameLogicThread.join(1500);

            if (renderThread != null) renderThread.join(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

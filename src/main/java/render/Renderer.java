package render;

import TheCellBeyond.Viewport;
import TheCellBeyond.internal.RenderingServer;
import eventviewer.EngineEventListener;
import eventviewer.event.Event;
import eventviewer.event.SceneEvent;
import org.joml.Matrix4f;
import render.commands.RenderCommand;
import render.text.TextBatch;
import render.texture.TextureManager;
import scene.Scene;

public class Renderer implements EngineEventListener {
    private static volatile Renderer instance;
    public static final int DefaultBatchCapacity = 512;
    private final TextureBatch textureBatch;
    private final TileBatch tileBatch;
    private final TextBatch textBatch;
    private final RenderCommandQueue queue;
    private final Matrix4f projectionMatrix = new Matrix4f().identity();
    private final Matrix4f viewMatrix = new Matrix4f().identity();
    private Scene currentScene;

    public static void init() {
        if (instance == null) instance = new Renderer();
    }

    private Renderer() {
        textureBatch = new TextureBatch(DefaultBatchCapacity);
        tileBatch = new TileBatch();
        textBatch = new TextBatch(DefaultBatchCapacity);
        queue = new RenderCommandQueue();
        register();
    }

    public static synchronized Renderer get() {
        return instance;
    }

    public void render() {
        adjustViewport();
        TextureManager.get().processCommands();
        RendererState state = RendererState.get();
        RenderCommand chainHead = RenderingServer.get().chainHead();
        if (chainHead == null) return;
        queue.collect(chainHead);
        tileBatch.beginFrame();
        textureBatch.beginFrame();
        textBatch.beginFrame();
        for (MeshEntry entry : queue.meshEntries) tileBatch.submit(entry.command(), entry.transform());
        for (RectEntry entry : queue.rectEntries) textureBatch.submit(entry.command(), entry.transform());
        for (TextEntry entry : queue.textEntries) textBatch.submit(entry.command(), entry.transform());
        tileBatch.endFrame();
        textureBatch.endFrame();
        textBatch.endFrame();
        if (RendererState.isNormalPass()) state.enableSpriteRendering();
        tileBatch.render(projectionMatrix, viewMatrix);
        textureBatch.render(projectionMatrix, viewMatrix);
        if (RendererState.isNormalPass()) state.enableTextRendering();
        textBatch.render(projectionMatrix, viewMatrix);
    }

    private void adjustViewport() {
        if (currentScene == null) return;
        Viewport viewport = currentScene.viewport();
        Matrix4f projection = viewport.getProjectionMatrix();
        Matrix4f view = viewport.getViewMatrix();
        if (projection != null) projectionMatrix.set(projection);
        if (view != null) viewMatrix.set(view);
    }

    @Override
    public void onEventEmit(Object object, Event event) {
        if (!(event instanceof SceneEvent sceneEvent)) return;
        switch(sceneEvent.type) {
            case SceneEntered -> {
                if (sceneEvent.scene == null) return;
                currentScene = sceneEvent.scene;
            }
            case SceneLeaved -> {
                if (sceneEvent.scene == null || currentScene != sceneEvent.scene) return;
                clearRenderData();
                currentScene = null;
            }
            default -> {}
        }
    }

    private void clearRenderData() {
        projectionMatrix.identity();
        viewMatrix.identity();
        tileBatch.clearSubmitted();
        textureBatch.clearSubmitted();
        textBatch.clearSubmitted();
        queue.clear();
    }
}

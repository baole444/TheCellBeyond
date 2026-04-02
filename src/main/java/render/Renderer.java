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

import java.util.HashSet;

public class Renderer implements EngineEventListener {
    private enum RenderMode {
        Sprite,
        Text
    }

    private static volatile Renderer instance;
    public static final int DefaultBatchCapacity = 512;
    private final TextureBatch textureBatch;
    private final TileBatch tileBatch;
    private final TextBatch textBatch;
    private final RenderCommandQueue queue;
    private final Matrix4f projectionMatrix = new Matrix4f().identity();
    private final Matrix4f viewMatrix = new Matrix4f().identity();
    private final HashSet<Integer> renderedTileZIndex = new HashSet<>();
    private final HashSet<Integer> renderedTextureZIndex = new HashSet<>();
    private final HashSet<Integer> renderedTextZIndex = new HashSet<>();
    private Scene currentScene;

    public static void init() {
        if (instance == null) instance = new Renderer();
    }

    private Renderer() {
        textureBatch = new TextureBatch();
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
        for (MeshEntry entry : queue.meshEntries) tileBatch.submit(entry.command(), entry.transform(), entry.previousTransform());
        for (RectEntry entry : queue.rectEntries) textureBatch.submit(entry.command(), entry.transform(), entry.previousTransform());
        for (TextEntry entry : queue.textEntries) textBatch.submit(entry.command(), entry.transform(), entry.previousTransform());
        tileBatch.endFrame();
        textureBatch.endFrame();
        textBatch.endFrame();
        if (RendererState.isNormalPass()) state.enableSpriteRendering();
        tileBatch.prepareRender(projectionMatrix, viewMatrix);
        textureBatch.prepareRender(projectionMatrix, viewMatrix);
        textBatch.prepareRender(projectionMatrix, viewMatrix);
        renderedTileZIndex.clear();
        renderedTextureZIndex.clear();
        renderedTextZIndex.clear();
        RenderMode currentMode = RenderMode.Sprite;
        for (RenderCommandQueue.ZIndexGroup group : queue.zIndexGroups) {
            boolean rendered = switch (group.batchType()) {
                case Tile -> !renderedTileZIndex.add(group.zIndex());
                case Sprite -> !renderedTextureZIndex.add(group.zIndex());
                case Text -> !renderedTextZIndex.add(group.zIndex());
            };
            if (rendered) continue;
            RenderMode mode = group.batchType() == RenderCommandQueue.BatchType.Text ? RenderMode.Text : RenderMode.Sprite;
            if (mode != currentMode) {
                switchRenderMode(state, mode);
                currentMode = mode;
            }
            switch (group.batchType()) {
                case Tile -> tileBatch.renderZIndex(group.zIndex());
                case Sprite -> textureBatch.renderZIndex(group.zIndex());
                case Text -> textBatch.renderZIndex(group.zIndex());
            }
        }
        if (RendererState.isNormalPass()) state.enableTextRendering();
    }

    private void switchRenderMode(RendererState state, RenderMode mode) {
        if (!RendererState.isNormalPass()) return;
        if (mode == RenderMode.Text) state.enableTextRendering();
        else state.enableSpriteRendering();
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

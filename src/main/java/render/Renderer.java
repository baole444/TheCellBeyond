package render;

import TheCellBeyond.GameObject;
import TheCellBeyond.Viewport;
import TheCellBeyond.internal.RenderUpdateSnapshot;
import components.Component;
import components.SpriteRenderer;
import components.TextRenderer;
import TheCellBeyond.TileMap;
import eventviewer.EngineEventCallback;
import eventviewer.EngineEventListener;
import eventviewer.event.Event;
import eventviewer.event.SceneEvent;
import org.joml.Matrix4f;
import render.text.TextBatch;
import render.texture.TextureManager;
import scene.Scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Renderer implements EngineEventListener {
    private static volatile Renderer instance;
    public static final int MAX_BATCH_SIZE = 512;
    private final List<TextureBatch> textureBatches = new ArrayList<>();
    private final List<TextBatch> textBatches = new ArrayList<>();
    private final List<TileBatch> tileBatches = new ArrayList<>();
    private final List<GameObject> switchZIndexQueue = new ArrayList<>();

    private final ConcurrentLinkedQueue<RenderUpdateSnapshot> updateSnapshots = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> pendingOperations = new ConcurrentLinkedQueue<>();

    private final Matrix4f projectionMatrix = new Matrix4f().identity();
    private final Matrix4f viewMatrix = new Matrix4f().identity();

    private Scene currentScene;
    private final AtomicBoolean awaitClearingRenderData = new AtomicBoolean(false);

    public static void init() {
        if (instance == null) instance = new Renderer();
    }

    private Renderer() {
        EngineEventCallback.register(this);
    }

    public static synchronized Renderer get() {
        return instance;
    }

    public void setMatrices(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        boolean changes = false;
        if (projectionMatrix != null && !projectionMatrix.equals(this.projectionMatrix)) {
            this.projectionMatrix.set(projectionMatrix);
            changes = true;
        }
        if (viewMatrix != null && !viewMatrix.equals(this.projectionMatrix)) {
            changes = true;
            this.viewMatrix.set(viewMatrix);
        }

        if (changes) updateBatchesMatrices();
    }

    private void updateBatchesMatrices() {
        for (TextureBatch textureBatch : textureBatches) {
            textureBatch.setProjectionMatrix(projectionMatrix);
            textureBatch.setViewMatrix(viewMatrix);
        }

        for (TextBatch textBatch : textBatches) {
            textBatch.setProjectionMatrix(projectionMatrix);
            textBatch.setViewMatrix(viewMatrix);
        }

        for (TileBatch tileBatch : tileBatches) {
            tileBatch.setProjectionMatrix(projectionMatrix);
            tileBatch.setViewMatrix(viewMatrix);
        }
    }

    public void render() {
        adjustViewport();
        TextureManager.get().processCommands();
        RendererState state = RendererState.get();
        processOperation();
        processSnapshot();

        if (RendererState.isNormalPass()) {
            state.enableSpriteRendering();
        }

        for (TileBatch tileBatch : tileBatches) {
            tileBatch.render();
        }

        for (TextureBatch textureBatch : textureBatches) {
            textureBatch.render();
        }

        if (RendererState.isNormalPass()) {
            state.enableTextRendering();
        }

        for (TextBatch textBatch : textBatches) {
            textBatch.render();
        }

        adjustZIndex();
    }

    private void adjustViewport() {
        if (currentScene == null) return;
        Viewport viewport = currentScene.viewport();
        setMatrices(viewport.getProjectionMatrix(), viewport.getViewMatrix());
    }

    void switchZIndex(GameObject go) {
        if (!switchZIndexQueue.contains(go)) switchZIndexQueue.add(go);
    }

    private void addGameObject(GameObject go) {
        if (go instanceof TileMap tileMap) {
            addTileMap(tileMap);
        }

        List<SpriteRenderer> sps = go.getComponents(SpriteRenderer.class);
        for (SpriteRenderer sprite : sps) {
            addSprite(sprite);
        }

        List<TextRenderer> ts = go.getComponents(TextRenderer.class);
        for (TextRenderer txt : ts) {
            addText(txt);
        }
    }

    private void addTileMap(TileMap map) {
        if (map == null) return;

        boolean isAdded = false;
        for (TileBatch tileBatch : tileBatches) {
            if (tileBatch.hasMap(map)) return;
            if (!tileBatch.hasSpace() || tileBatch.zIndex() != map.globalZIndex()) continue;

            tileBatch.loadTileMap(map);
            isAdded = true;
            break;
        }

        if (isAdded) return;
        TileBatch newTileBatch = new TileBatch(map, map.globalZIndex(), this);
        newTileBatch.start();

        if (projectionMatrix != null) newTileBatch.setProjectionMatrix(projectionMatrix);
        if (viewMatrix != null) newTileBatch.setViewMatrix(viewMatrix);

        tileBatches.add(newTileBatch);
        Collections.sort(tileBatches);
    }

    private void addSprite(SpriteRenderer sprite) {
        if (sprite == null) return;

        boolean isAdded = false;
        for (TextureBatch textureBatch : textureBatches) {
            if (textureBatch.hasSprite(sprite)) return;
            if (!textureBatch.hasSpace() || textureBatch.zIndex() != sprite.globalZIndex()) continue;

            Texture t = sprite.texture();
            if (t == null || textureBatch.hasTexture(t) || textureBatch.isTextureCapacityValid()) {
                textureBatch.loadSprite(sprite);
                isAdded = true;
                break;
            }
        }

        if (isAdded) return;
        TextureBatch newTextureBatch = new TextureBatch(MAX_BATCH_SIZE, sprite.globalZIndex(), this);
        newTextureBatch.start();

        if (projectionMatrix != null) newTextureBatch.setProjectionMatrix(projectionMatrix);
        if (viewMatrix != null) newTextureBatch.setViewMatrix(viewMatrix);

        textureBatches.add(newTextureBatch);
        newTextureBatch.loadSprite(sprite);
        Collections.sort(textureBatches);
    }

    private void addText(TextRenderer text) {
        if (text == null) return;

        boolean isAdded = false;
        int zIndex = text.globalZIndex();

        for (TextBatch batch : textBatches) {
            if (batch.hasSpace() && batch.getzIndex() == zIndex) {
                batch.add(text);
                isAdded = true;
                break;
            }
        }

        if (isAdded) return;
        TextBatch newBatch = new TextBatch(MAX_BATCH_SIZE, zIndex);
        newBatch.start();

        if (projectionMatrix != null) newBatch.setProjectionMatrix(projectionMatrix);
        if (viewMatrix != null) newBatch.setViewMatrix(viewMatrix);

        textBatches.add(newBatch);
        newBatch.add(text);
        Collections.sort(textBatches);
    }

    private void destroyObject(GameObject go) {
        if (go instanceof TileMap tileMap) {
            for (TileBatch tileBatch : tileBatches) {
                if (tileBatch.removeIfExist(tileMap)) break;
            }
        }

        if (go.getFirstComponent(SpriteRenderer.class) != null) {
            for (TextureBatch textureBatch : textureBatches) {
                if (textureBatch.removeIfExist(go)) break;
            }
        }

        if (go.getFirstComponent(TextRenderer.class) != null) {
            for (TextBatch textBatch : textBatches) {
                if (textBatch.removeIfExist(go)) break;
            }
        }
    }

    private void removeComponent(Component component) {
        if (component == null) return;

        if (component instanceof SpriteRenderer spriteRenderer) {
            for (TextureBatch textureBatch : textureBatches) {
                if (textureBatch.removeIfExist(spriteRenderer)) return;
            }
        }

        if (component instanceof TextRenderer textRenderer) {
            for (TextBatch textBatch : textBatches) {
                if (textBatch.removeComponent(textRenderer)) return;
            }
        }
    }

    private void adjustZIndex() {
        if (switchZIndexQueue.isEmpty()) return;
        List<GameObject> updateList = new ArrayList<>(switchZIndexQueue);
        switchZIndexQueue.clear();

        for (GameObject go : updateList) if (!go.isRemoved()) addGameObject(go);
    }

    private void processOperation() {
        Runnable operation;
        while ((operation = pendingOperations.poll()) != null) operation.run();
    }

    private void processSnapshot() {
        if (awaitClearingRenderData.get()) {
            clearRenderData();
            awaitClearingRenderData.set(false);
            return;
        }

        RenderUpdateSnapshot snapshot = updateSnapshots.poll();
        if (snapshot == null) return;
        for (GameObject go : snapshot.updateObjects()) if (!go.isRemoved()) addGameObject(go);
    }

    @Override
    public void onEventEmit(Object object, Event event) {
        if (!(event instanceof SceneEvent sceneEvent)) return;
        switch(sceneEvent.type) {
            case SceneEntered -> onSceneStart(sceneEvent.scene);
            case SceneLeaved -> onSceneLeave(sceneEvent.scene);
            case ObjectAdded -> onObjectAdded(sceneEvent.scene, sceneEvent.params);
            case ObjectUpdated -> onObjectUpdated(sceneEvent.scene, sceneEvent.params);
            case ObjectRemoved -> onObjectRemoved(sceneEvent.scene, sceneEvent.params);
            case ComponentRemoved -> onComponentRemoved(sceneEvent.scene, sceneEvent.params);
        }
    }

    private void onSceneStart(Scene scene) {
        if (scene == null) return;
        awaitClearingRenderData.set(true);
        currentScene = scene;
    }

    private void onSceneLeave(Scene scene) {
        if (scene == null || currentScene != scene) return;
        awaitClearingRenderData.set(true);
        currentScene = null;
    }

    private void onObjectAdded(Scene scene, List<Object> params) {
        if (scene != currentScene) return;
        if (params.isEmpty()) return;
        Object param = params.getFirst();
        if (param instanceof GameObject go) pendingOperations.offer(() -> addGameObject(go));
    }

    private void onObjectUpdated(Scene scene, List<Object> params) {
        if (scene != currentScene) return;
        if (params.isEmpty()) return;
        Object param = params.getFirst();
        if (param instanceof RenderUpdateSnapshot snapshot) updateSnapshots.offer(snapshot);
    }

    private void onObjectRemoved(Scene scene, List<Object> params) {
        if (scene != currentScene) return;
        if (params.isEmpty()) return;
        Object param = params.getFirst();
        if (param instanceof GameObject go) pendingOperations.offer(() -> destroyObject(go));
    }

    private void onComponentRemoved(Scene scene, List<Object> params) {
        if (scene != currentScene) return;
        if (params.isEmpty()) return;
        Object param = params.getFirst();
        if (param instanceof Component component) pendingOperations.offer(() -> removeComponent(component));
    }

    private void clearRenderData() {
        projectionMatrix.identity();
        viewMatrix.identity();
        textureBatches.clear();
        tileBatches.clear();
        textBatches.clear();
        updateSnapshots.clear();
        switchZIndexQueue.clear();
    }
}

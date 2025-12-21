package render;

import TheCellBeyond.GameObject;
import TheCellBeyond.internal.RenderingSnapshot;
import components.Component;
import components.SpriteRenderer;
import components.TextRenderer;
import components.TileMap;
import org.joml.Matrix4f;
import render.text.TextBatch;
import render.texture.TextureManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Renderer {
    private static volatile Renderer instance;
    public static final int MAX_BATCH_SIZE = 512;
    private final List<TextureBatch> textureBatches;
    private final List<TextBatch> textBatches;
    private final List<TileBatch> tileBatches;

    private final ConcurrentLinkedQueue<RenderingSnapshot> snapshots;
    private final List<GameObject> switchZIndexQueue;

    private Matrix4f projectionMatrix = null;
    private Matrix4f viewMatrix = null;

    public void setMatrices(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        this.projectionMatrix = projectionMatrix;
        this.viewMatrix = viewMatrix;

        updateBatchesMatrices();
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

    private Renderer() {
        textureBatches = new ArrayList<>();
        textBatches = new ArrayList<>();
        tileBatches = new ArrayList<>();
        snapshots = new ConcurrentLinkedQueue<>();
        switchZIndexQueue = new ArrayList<>();
    }

    public static synchronized Renderer get() {
        if (instance == null) instance = new Renderer();
        return instance;
    }

    public static synchronized void clearData() {
        instance = null;
    }

    public void render() {
        TextureManager.get().processCommands();

        RendererState state = RendererState.get();

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

    public void queueSnapshot(RenderingSnapshot snapshot) {
        if (snapshot != null) snapshots.offer(snapshot);
    }

    void switchZIndex(GameObject go) {
        if (!switchZIndexQueue.contains(go)) switchZIndexQueue.add(go);
    }

    private void addGameObject(GameObject go) {
        List<TileMap> tms = go.getComponents(TileMap.class);
        for (TileMap tileMap : tms) {
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

        if (!isAdded) {
            TileBatch newTileBatch = new TileBatch(map, map.globalZIndex(), this);
            newTileBatch.start();

            if (projectionMatrix != null) newTileBatch.setProjectionMatrix(projectionMatrix);
            if (viewMatrix != null) newTileBatch.setViewMatrix(viewMatrix);

            tileBatches.add(newTileBatch);
            Collections.sort(tileBatches);
        }
    }

    private void addSprite(SpriteRenderer sprite) {
        if (sprite == null) return;

        boolean isAdded = false;
        for (TextureBatch textureBatch : textureBatches) {
            if (textureBatch.hasSprite(sprite)) return;
            if (!textureBatch.hasSpace() || textureBatch.zIndex() != sprite.globalZIndex()) continue;

            Texture t = sprite.getTexture();
            if (t == null || textureBatch.hasTexture(t) || textureBatch.isTextureCapacityValid()) {
                textureBatch.loadSprite(sprite);
                isAdded = true;
                break;
            }
        }

        if (!isAdded) {
            TextureBatch newTextureBatch = new TextureBatch(MAX_BATCH_SIZE, sprite.globalZIndex(), this);
            newTextureBatch.start();

            if (projectionMatrix != null) newTextureBatch.setProjectionMatrix(projectionMatrix);
            if (viewMatrix != null) newTextureBatch.setViewMatrix(viewMatrix);

            textureBatches.add(newTextureBatch);
            newTextureBatch.loadSprite(sprite);
            Collections.sort(textureBatches);
        }
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

        if (!isAdded) {
            TextBatch newBatch = new TextBatch(MAX_BATCH_SIZE, zIndex);
            newBatch.start();

            if (projectionMatrix != null) newBatch.setProjectionMatrix(projectionMatrix);
            if (viewMatrix != null) newBatch.setViewMatrix(viewMatrix);

            textBatches.add(newBatch);
            newBatch.add(text);
            Collections.sort(textBatches);
        }
    }

    private void destroyObject(GameObject go) {
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

        if (go.getFirstComponent(TileMap.class) != null) {
            for (TileBatch tileBatch : tileBatches) {
                if (tileBatch.removeIfExist(go)) break;
            }
        }
    }

    private void removeComponent(Component component) {
        if (component == null) return;

        if (component instanceof TileMap tileMap) {
            for (TileBatch tileBatch : tileBatches) {
                if (tileBatch.removeIfExist(tileMap)) return;
            }
        }

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

    private void processSnapshot() {
        RenderingSnapshot snapshot = snapshots.poll();
        if (snapshot == null) return;

        for (Component component : snapshot.removeComponents()) removeComponent(component);
        for (GameObject go : snapshot.removeObjects()) destroyObject(go);
        for (GameObject go : snapshot.updateObjects()) if (!go.isRemoved()) addGameObject(go);
    }
}

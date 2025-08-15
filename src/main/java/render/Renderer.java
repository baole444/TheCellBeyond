package render;

import TheCellBeyond.GameObject;
import components.SpriteRenderer;
import components.TextRenderer;
import org.joml.Matrix4f;
import render.text.FontManager;
import render.text.TextBatch;
import render.texture.TextureManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Renderer {
    private final int MAX_BATCH_SIZE = 1000;
    private final List<Batch> textureBatches;
    private final List<TextBatch> textBatches;

    private final List<GameObject> updatedGameObjects;
    private final List<GameObject> removedGameObjects;

    private Matrix4f projectionMatrix = null;
    private Matrix4f viewMatrix = null;

    public void setMatrices(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        this.projectionMatrix = projectionMatrix;
        this.viewMatrix = viewMatrix;

        updateBatchesMatrices();
    }

    private void updateBatchesMatrices() {
        for (Batch batch : textureBatches) {
            batch.setProjectionMatrix(projectionMatrix);
            batch.setViewMatrix(viewMatrix);
        }

        for (TextBatch textBatch : textBatches) {
            textBatch.setProjectionMatrix(projectionMatrix);
            textBatch.setViewMatrix(viewMatrix);
        }
    }

    public Renderer() {
        this.textureBatches = new ArrayList<>();
        this.textBatches = new ArrayList<>();
        this.updatedGameObjects = new ArrayList<>();
        this.removedGameObjects = new ArrayList<>();
    }

    public void render() {
        TextureManager.get().processCommands();

        FontManager.get().updateFontTextures(); // Within a render loop for a GL context, this can only be call once.

        RendererState state = RendererState.get();

        if (state.getCurrentPass() == RendererState.RenderPass.NORMAL) {
            state.enableSpriteRendering();
        }

        for (Batch batch : textureBatches) {
            batch.render();
        }

        if (state.getCurrentPass() == RendererState.RenderPass.NORMAL) {
            state.enableTextRendering();
        }

        for (TextBatch textBatch : textBatches) {
            textBatch.render();
        }

        updateBatches();
    }

    /**
     * This is just an alias for {@link Renderer#queueObjectForUpdate(GameObject)}.
     * @param go game object to add.
     */
    public void queueObjectForAddition(GameObject go) {
        queueObjectForUpdate(go);
    }

    // Add a game object to the removal list and remove it from the update list.
    public void queueObjectForRemoval(GameObject go) {
        updatedGameObjects.remove(go);
        if (!removedGameObjects.contains(go)) removedGameObjects.add(go);
    }

    public void queueObjectForUpdate(GameObject go) {
        if (!updatedGameObjects.contains(go) && !removedGameObjects.contains(go)) updatedGameObjects.add(go);
    }

    private void addGameObject(GameObject go) {
        List<SpriteRenderer> sps = go.getComponents(SpriteRenderer.class);
        for (SpriteRenderer sprite : sps) {
            addSprite(sprite);
        }

        List<TextRenderer> ts = go.getComponents(TextRenderer.class);
        for (TextRenderer txt : ts) {
            addText(txt);
        }
    }

    private void addSprite(SpriteRenderer sprite) {
        if (sprite == null) return;

        boolean isAdded = false;
        for (Batch batch: textureBatches) {
            if (batch.hasSpace() && batch.zIndex() == sprite.getzIndex()) {
                Texture t = sprite.getTexture();
                if (t == null || (batch.hasTexture(t) || batch.isTextureCapacityValid())) {
                    batch.loadSprite(sprite);
                    isAdded = true;
                    break;
                }
            }
        }

        if (!isAdded) {
            Batch newBatch = new Batch(MAX_BATCH_SIZE, sprite.getzIndex(), this);
            newBatch.start();

            if (projectionMatrix != null) newBatch.setProjectionMatrix(projectionMatrix);
            if (viewMatrix != null) newBatch.setViewMatrix(viewMatrix);

            textureBatches.add(newBatch);
            newBatch.loadSprite(sprite);
            Collections.sort(textureBatches);
        }
    }

    private void addText(TextRenderer text) {
        if (text == null) return;

        boolean isAdded = false;
        int zIndex = text.getzIndex();

        for (TextBatch batch : textBatches) {
            if (batch.hasRoom() && batch.getzIndex() == zIndex) {
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
            for (Batch batch : textureBatches) {
                if (batch.removeIfExist(go)) {
                    return;
                }
            }
        }

        TextRenderer textRenderer = go.getFirstComponent(TextRenderer.class);
        if (textRenderer != null) {
            for (TextBatch textBatch : textBatches) {
                if (textBatch.removeComponent(textRenderer)) {
                    return;
                }
            }
        }
    }

    private void updateBatches() {
        for (GameObject go: removedGameObjects) {
            destroyObject(go);
        }

        for (GameObject go : updatedGameObjects) {
            if (!go.isRemoved()) addGameObject(go);
        }

        updatedGameObjects.clear();
        removedGameObjects.clear();
    }

    public void cleanup() {
        TextureManager.get().cleanup();
    }
}

package render;

import TheCellBeyond.GameObject;
import components.Component;
import components.SpriteRenderer;
import components.TextRenderer;
import org.joml.Matrix4f;
import render.text.TextBatch;
import render.texture.TextureManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Renderer {
    private static volatile Renderer instance;
    public static final int MAX_BATCH_SIZE = 512;
    private final List<Batch> textureBatches;
    private final List<TextBatch> textBatches;

    private final List<GameObject> updatedGameObjects;
    private final List<GameObject> removedGameObjects;
    private final List<Component> removedComponents;

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

    private Renderer() {
        textureBatches = new ArrayList<>();
        textBatches = new ArrayList<>();
        updatedGameObjects = new ArrayList<>();
        removedGameObjects = new ArrayList<>();
        removedComponents = new ArrayList<>();
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

        if (RendererState.isNormalPass()) {
            state.enableSpriteRendering();
        }

        for (Batch batch : textureBatches) {
            batch.render();
        }

        if (RendererState.isNormalPass()) {
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

    public void queueComponentForRemoval(Component component) {
        if (!removedComponents.contains(component)) removedComponents.add(component);
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
            if (batch.hasSprite(sprite)) return;

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

        List<TextRenderer> textRenderers = go.getComponents(TextRenderer.class);
        if (!textRenderers.isEmpty()) {
            for (TextRenderer t : textRenderers) {
                if (t == null) continue;
                for (TextBatch textBatch : textBatches) {
                    if (textBatch.removeComponent(t)) return;
                }
            }
        }
    }

    private void removeComponent(Component component) {
        if (component == null) return;

        if (component instanceof SpriteRenderer spriteRenderer) {
            for (Batch batch : textureBatches) {
                if (batch.removeIfExist(spriteRenderer)) return;
            }
        }

        if (component instanceof TextRenderer textRenderer) {
            for (TextBatch textBatch : textBatches) {
                if (textBatch.removeComponent(textRenderer)) return;
            }
        }
    }

    private void updateBatches() {
        List<GameObject> updateList = new ArrayList<>(updatedGameObjects);
        updatedGameObjects.clear();
        List<GameObject> removeList = new ArrayList<>(removedGameObjects);
        removedGameObjects.clear();
        List<Component> removeComponentList = new ArrayList<>(removedComponents);
        removedComponents.clear();

        for (Component c : removeComponentList) {
            removeComponent(c);
        }

        for (GameObject go: removeList) {
            destroyObject(go);
        }

        for (GameObject go : updateList) {
            if (!go.isRemoved()) addGameObject(go);
        }
    }
}

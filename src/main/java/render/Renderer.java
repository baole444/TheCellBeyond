package render;

import TheCellBeyond.GameObject;
import components.Component;
import components.SpriteRender;
import components.StateEngine;
import components.TextComponent;
import org.joml.Matrix4f;
import render.text.FontManager;
import render.text.TextBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Renderer {
    private final int MAX_BATCH_SIZE = 1000;
    private final List<Batch> textureBatches;
    private final List<TextBatch> textBatches;

    private final ConcurrentLinkedQueue<GameObject> updatedGameObjects;

    // This track if we already queue a game object for update this frame.
    private final ConcurrentHashMap<Integer, Boolean> queuedForUpdate;

    private final ConcurrentLinkedQueue<Integer> removedGameObjectUIDs;

    // This hash map is a deep copy of game objects from game logic thread.
    // All batches methods will reference these objects instead of live game objects.
    private final ConcurrentHashMap<Integer, GameObject> internalGameObjects;

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

        this.updatedGameObjects = new ConcurrentLinkedQueue<>();
        this.queuedForUpdate = new ConcurrentHashMap<>();

        this.removedGameObjectUIDs = new ConcurrentLinkedQueue<>();

        this.internalGameObjects = new ConcurrentHashMap<>();
    }

    private void addSprite(SpriteRender sprite) {
        boolean isAdded = false;
        for (Batch batch: textureBatches) {
            if (batch.hasSpace() && batch.zIndex() == sprite.gameObject.transform.zIndex) {
                Texture t = sprite.getTexture();
                if (t == null || (batch.hasTexture(t) || batch.isTextureCapacityValid())) {
                    batch.loadSprite(sprite);
                    isAdded = true;
                    break;
                }
            }
        }

        if (!isAdded) {
            Batch newBatch = new Batch(MAX_BATCH_SIZE, sprite.gameObject.transform.zIndex, this);
            newBatch.start();

            if (projectionMatrix != null) newBatch.setProjectionMatrix(projectionMatrix);
            if (viewMatrix != null) newBatch.setViewMatrix(viewMatrix);

            textureBatches.add(newBatch);
            newBatch.loadSprite(sprite);
            Collections.sort(textureBatches);
        }
    }

    private void addText(TextComponent textComponent) {
        boolean isAdded = false;
        int zIndex = textComponent.gameObject.transform.zIndex;

        for (TextBatch batch : textBatches) {
            if (batch.hasRoom() && batch.getzIndex() == zIndex) {
                batch.add(textComponent);
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
            newBatch.add(textComponent);
            Collections.sort(textBatches);
        }
    }

    public void render() {
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
        if (go == null) return;

        int uid = go.getUID();

        // Remove tracking data.
        queuedForUpdate.remove(uid);

        // Remove from update queue.
        updatedGameObjects.removeIf(object -> object.getUID() == uid);

        // Add this to the removal queue so it cannot be added again in the same frame.
        removedGameObjectUIDs.offer(uid);
    }

    // Add a game object to the update list so it can be updated with Renderer's internal list.
    // If the object is queued for removal, it cannot be queued again.
    public void queueObjectForUpdate(GameObject go) {
        if (go == null) return;

        int uid = go.getUID();

        // Object is on its way to be removed, will not queue it again.
        if (removedGameObjectUIDs.contains(uid)) return;

        // If this object is not tracked already, queue it for update.
        if (queuedForUpdate.putIfAbsent(uid, Boolean.TRUE) == null) updatedGameObjects.offer(go);
    }

    private void addGameObject(GameObject go) {
        GameObject internalGO = getInternalGameObject(go);


        SpriteRender spr = internalGO.getComponent(SpriteRender.class);
        if (spr != null) {
            addSprite(spr);
        }

        TextComponent text = internalGO.getComponent(TextComponent.class);
        if (text != null) {
            addText(text);
        }
    }


    private void destroyObject(int uid) {
        GameObject go = internalGameObjects.get(uid);

        if (go == null) return;

        if (go.getComponent(SpriteRender.class) != null) {
            for (Batch batch : textureBatches) {
                if (batch.removeIfExist(go)) break;
            }
        }

        TextComponent textComponent = go.getComponent(TextComponent.class);
        if (textComponent != null) {
            for (TextBatch textBatch : textBatches) {
                if (textBatch.removeComponent(textComponent)) break;
            }
        }

        internalGameObjects.remove(uid);
    }

    private GameObject getInternalGameObject(GameObject og) {
        int uid = og.getUID();

        GameObject internal = internalGameObjects.get(uid);

        if (internal == null) {
            internal = og.copy();
            internalGameObjects.put(uid, internal);
        } else {
            updateInternalGameObject(internal, og);
        }

        return internal;
    }

    private void updateInternalGameObject(GameObject internalObject, GameObject og) {
        boolean needRebatch = internalObject.transform != null &&
                og.transform != null &&
                internalObject.transform.zIndex != og.transform.zIndex;

        if (needRebatch) {
            int uid = internalObject.getUID();
            destroyObject(uid);
            GameObject copy = og.copy();
            copy.setUID(uid);
            addGameObject(copy);
            return;
        }

        updateObjectComponents(internalObject, og);
    }

    private void updateObjectComponents(GameObject internal, GameObject og) {
        List<Component> ogComponents = og.getComponents();

        for (Component ogC : ogComponents) {
            Component internalC = internal.getComponent(ogC.getClass());

            if (internalC != null) internalC.copyFrom(ogC);
        }
    }

    private void updateBatches() {
        Integer removedUID;

        while ((removedUID = removedGameObjectUIDs.poll()) != null) {
            destroyObject(removedUID);
            queuedForUpdate.remove(removedUID);
        }

        GameObject toUpdate;
        while ((toUpdate = updatedGameObjects.poll()) != null) {
            int uid = toUpdate.getUID();
            if (!removedGameObjectUIDs.contains(uid)) addGameObject(toUpdate);

            queuedForUpdate.remove(uid);
        }
    }
}

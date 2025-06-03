package render;

import TheCellBeyond.GameObject;
import components.SpriteRender;
import components.TextComponent;
import render.text.FontManager;
import render.text.TextBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Renderer {
    private final int MAX_BATCH_SIZE = 1000;
    private final List<Batch> textureBatches;
    private final List<TextBatch> textBatches;

    private final List<GameObject> updatedGameObjects;
    private final List<GameObject> removedGameObjects;

    public Renderer() {
        this.textureBatches = new ArrayList<>();
        this.textBatches = new ArrayList<>();
        this.updatedGameObjects = new ArrayList<>();
        this.removedGameObjects = new ArrayList<>();
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

    public void queueObjectForRemoval(GameObject go) {
        updatedGameObjects.remove(go);
        if (!removedGameObjects.contains(go)) removedGameObjects.add(go);
    }

    public void queueObjectForUpdate(GameObject go) {
        if (!updatedGameObjects.contains(go) && !removedGameObjects.contains(go)) updatedGameObjects.add(go);
    }

    private void addGameObject(GameObject go) {
        SpriteRender spr = go.getComponent(SpriteRender.class);
        if (spr != null) {
            addSprite(spr);
        }

        TextComponent text = go.getComponent(TextComponent.class);
        if (text != null) {
            addText(text);
        }
    }


    private void destroyObject(GameObject go) {
        if (go.getComponent(SpriteRender.class) != null) {
            for (Batch batch : textureBatches) {
                if (batch.removeIfExist(go)) {
                    return;
                }
            }
        }

        TextComponent textComponent = go.getComponent(TextComponent.class);
        if (textComponent != null) {
            for (TextBatch textBatch : textBatches) {
                if (textBatch.removeComponent(textComponent)) {
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
}

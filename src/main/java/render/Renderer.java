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
    private final List<Batch> batches;
    private final List<TextBatch> textBatches;

    public Renderer() {
        this.batches = new ArrayList<>();
        this.textBatches = new ArrayList<>();
    }

    public void add(GameObject go) {
        SpriteRender spr = go.getComponent(SpriteRender.class);
        if (spr != null) {
            add(spr);
        }

        TextComponent text = go.getComponent(TextComponent.class);
        if (text != null) {
            addText(text);
        }
    }

    private void add(SpriteRender sprite) {
        boolean isAdd = false;
        for (Batch batch: batches) {
            if (batch.hasSpace() && batch.zIndex() == sprite.gameObject.transform.zIndex) {
                Texture t = sprite.getTexture();
                if (t == null || (batch.isTex(t) || batch.isTexCapValid())) {
                    batch.loadSprite(sprite);
                    isAdd = true;
                    break;
                }
            }
        }

        if (!isAdd) {
            Batch newBatch = new Batch(MAX_BATCH_SIZE, sprite.gameObject.transform.zIndex, this);
            newBatch.start();
            batches.add(newBatch);
            newBatch.loadSprite(sprite);
            Collections.sort(batches);
        }
    }

    private void addText(TextComponent textComponent) {
        boolean isAdd = false;
        int zIndex = textComponent.gameObject.transform.zIndex;

        for (TextBatch batch : textBatches) {
            if (batch.hasRoom() && batch.getzIndex() == zIndex) {
                batch.add(textComponent);
                isAdd = true;
                break;
            }
        }

        if (!isAdd) {
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

        // TODO: Need to fix cocurrent exception when changing to enhanced for loop.
        for (int i = 0; i < batches.size(); i++) {
            Batch batch = batches.get(i);
            batch.render();
        }

        if (state.getCurrentPass() == RendererState.RenderPass.NORMAL) {
            state.enableTextRendering();
        }

        for (TextBatch textBatch : textBatches) {
            textBatch.render();
        }
    }

    public void destroyObject(GameObject go) {
        if (go.getComponent(SpriteRender.class) != null) {
            for (Batch batch : batches) {
                if (batch.removeWhenExist(go)) {
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
}

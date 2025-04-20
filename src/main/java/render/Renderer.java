package render;

import TCB_Field.GameObject;
import components.SpriteRender;
import components.TextComponent;
import render.text.TextBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Renderer {

    private final int MAX_BATCH_SIZE = 1000;
    private List<Batch> batches;
    private List<TextBatch> textBatches;

    private static Shader instShader;

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
                Texture t = sprite.loadTexture();
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
        if (textComponent.getFont() == null) return;

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

    public static void setShader(Shader shader) {
        instShader = shader;
    }

    public static Shader loadShader() {
        return instShader;
    }

    public void render() {
        instShader.use();
        for (Batch batch : batches) {
            batch.render();
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

package render;

import TCB_Field.GameObject;
import components.SpriteRender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Renderer {
    private final int MAX_BATCH_SIZE = 1000;
    private List<Batch> batches;

    public Renderer() {
        this.batches = new ArrayList<>();
    }

    public void add(GameObject go) {
        SpriteRender spr = go.getComponent(SpriteRender.class);
        if (spr != null) {
            add(spr);
        }
    }

    private void add(SpriteRender sprite) {
        boolean isAdd = false;
        for (Batch batch: batches) {
            if (batch.hasSpace() && batch.zIndex() == sprite.gameObject.zIndex()) {
                Texture t = sprite.loadTexture();
                if (t == null || (batch.isTex(t) || batch.isTexCapValid())) {
                    batch.loadSprite(sprite);
                    isAdd = true;
                    break;
                }
            }
        }

        if (!isAdd) {
            Batch newBatch = new Batch(MAX_BATCH_SIZE, sprite.gameObject.zIndex());
            newBatch.start();
            batches.add(newBatch);
            newBatch.loadSprite(sprite);
            Collections.sort(batches);
        }
    }

    public void render() {
        for (Batch batch : batches) {
            batch.render();
        }
    }

}

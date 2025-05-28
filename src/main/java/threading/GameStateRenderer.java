package threading;

import TheCellBeyond.GameObject;
import TheCellBeyond.Transform;
import TheCellBeyond.Viewport;
import components.SpriteRender;
import components.TextComponent;
import render.Batch;
import render.Shader;
import render.Texture;
import render.text.FontManager;
import render.text.TextBatch;
import threading.states.GameObjectState;
import threading.states.GameState;
import threading.states.SpriteRenderState;
import threading.states.TextComponentState;
import utility.AssetsPool;

import java.util.*;

public class GameStateRenderer {
    private final Viewport viewport;
    private final Shader defaultShader;
    private final Shader selectionShader;

    public GameStateRenderer(Viewport viewport, Shader defaultShader, Shader selectionShader) {
        this.viewport = viewport;
        this.defaultShader = defaultShader;
        this.selectionShader = selectionShader;
    }

    public void render(GameState state) {
        FontManager.get().updateFontTextures();

        Map<Integer, Batch> spriteBatches = new HashMap<>();
        Map<Integer, TextBatch> textBatches = new HashMap<>();

        for (GameObjectState objectState : state.getGameObjectStates()) {
            if (objectState.isRemoved()) {
                continue;
            }

            int zIndex = objectState.getTransform().zIndex;

            SpriteRenderState spriteState = objectState.getSpriteRenderState();
            if (spriteState != null && spriteState.getTexturePath() != null) {
                Batch batch = spriteBatches.computeIfAbsent(zIndex, index -> {
                    Batch newBatch = new Batch(1000, index, null);
                    newBatch.start();
                    return newBatch;
                });

                SpriteRender tmpSpr = createNewTempSpriteRender(objectState, spriteState);

                if (tmpSpr != null) {
                    batch.loadSprite(tmpSpr);
                }
            }

            TextComponentState textState = objectState.getTextComponentState();
            if (textState != null) {
                TextBatch textBatch = textBatches.computeIfAbsent(zIndex, index -> {
                    TextBatch newBatch = new TextBatch(1000, index);
                    newBatch.start();

                    newBatch.setProjectionMatrix(viewport.getProjectionMatrix());
                    newBatch.setViewMatrix(viewport.getViewMatrix());

                    return newBatch;
                });

                TextComponent tmpTextCpt = createNewTempTextComponent(objectState, textState);

                if (tmpTextCpt != null) {
                    textBatch.add(tmpTextCpt);
                }
            }
        }

        List<Batch> sortedSpriteBatches = new ArrayList<>(spriteBatches.values());
        Collections.sort(sortedSpriteBatches);

        List<TextBatch> sortedTextBatches = new ArrayList<>(textBatches.values());
        Collections.sort(sortedTextBatches);

        for (Batch batch : sortedSpriteBatches) {
            batch.render();
        }

        for (TextBatch textBatch : sortedTextBatches) {
            textBatch.render();
        }
    }
}

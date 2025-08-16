package editor;

import TheCellBeyond.GameObject2D;
import components.IsNotSerialized;
import components.SpatialComponent;
import components.SpriteRenderer;
import org.joml.Vector4f;
import render.texture.Sprite;
import render.texture.SpriteSheet;
import utility.AssetsPool;
import utility.Settings;
import utility.WorldUnit;

public class Indicator extends SpriteRenderer implements IsNotSerialized {
    private static final String PATH = "engine://assets/textures/indicator.png";

    private transient SpriteSheet sheet;
    private transient boolean isInitialized = false;

    @Override
    protected void additionalStartLogic() {
        super.additionalStartLogic();
        initIndicator();
    }

    private void initIndicator() {
        if (isInitialized
                || gameObject == null
                || !gameObject.isSerialize()
                || !(gameObject instanceof GameObject2D)
        ) return;

        try {
            if (!AssetsPool.hasSpriteSheet(PATH)) {
                AssetsPool.addSpriteSheet(PATH,
                        new SpriteSheet(AssetsPool.loadTexture(PATH), 12, 12, 1, 0)
                );
            }

            sheet = AssetsPool.loadSpriteSheet(PATH);
            setColor(new Vector4f(1.0f, 1.0f, 1.0f, 0.8f));
            completeInit();
        } catch (Exception e) {
            System.err.println("Failed to initialize Indicator: " + e.getMessage());
        }
    }

    private void completeInit() {
        if (sheet == null) return;

        Sprite sprite = sheet.spriteIndex(0);

        if (sprite == null) return;

        setSprite(sprite);

        isInitialized = true;
    }

    @Override
    public void editorUpdate(float dt) {
        if (!gameObject.isSerialize()) return;

        if (sheet == null) {
            initIndicator();
            return;
        }

        if (!isInitialized) {
            completeInit();
            return;
        }

        super.editorUpdate(dt);
    }

    // Object's indicator will not be show in runtime mode (update).
    // Don't override update method.
    // It shouldn't also be edited in the properties windows too.
    // Override imgui to do nothing to prevent this
    @Override
    public void imgui() {}
}

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

public class Indicator extends SpatialComponent implements IsNotSerialized {
    private static final String PATH = "engine://assets/textures/indicator.png";
    private static final float SIZE = 12 * Settings.WORLD_SCALE_FACTOR;

    private transient SpriteSheet sheet;
    private transient SpriteRenderer indicatorRenderer;
    private transient boolean isInitialized = false;

    @Override
    public void start() {
        initIndicator();
    }

    private void initIndicator() {
        if (isInitialized || gameObject == null || !gameObject.isSerialize() || !(gameObject instanceof GameObject2D)) return;

        try {
            if (!AssetsPool.hasSpriteSheet(PATH)) {
                AssetsPool.addSpriteSheet(PATH,
                        new SpriteSheet(AssetsPool.loadTexture(PATH), 12, 12, 1, 0)
                );
            }

            sheet = AssetsPool.loadSpriteSheet(PATH);

            indicatorRenderer = new SpriteRenderer();
            indicatorRenderer.setColor(new Vector4f(1.0f, 1.0f, 1.0f, 0.8f));
            indicatorRenderer.gameObject = this.gameObject;
            indicatorRenderer.start();

            completeInit();
        } catch (Exception e) {
            System.err.println("Failed to initialize Indicator: " + e.getMessage());
        }
    }

    private void completeInit() {
        if (sheet == null) return;

        Sprite sprite = sheet.spriteIndex(0);

        if (sprite == null) return;

        indicatorRenderer.setSprite(sprite);

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

        if (indicatorRenderer != null) {
            indicatorRenderer.editorUpdate(dt);
        }
    }

    // Object's indicator will not be show in runtime mode (update).
    // Don't override update method.
    // It shouldn't also be edited in the properties windows too.
    // Override imgui to do nothing to prevent this
    @Override
    public void imgui() {}

    public SpriteRenderer getIndicatorRenderer() {
        return indicatorRenderer;
    }

    /**
     * Set the tint of the indicator object.
     * @param color the color vector in RGBA (0 -> 1)
     */
    public void setIndicatorColor(Vector4f color) {
        if (indicatorRenderer != null) {
            indicatorRenderer.setColor(color);
        } else {
            System.err.println("Warning: updating color on indicator that is not fully initialized for object: " + gameObject.toString());
        }
    }
}

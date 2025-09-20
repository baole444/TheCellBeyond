package editor;

import TheCellBeyond.GameObject2D;
import components.NotSerializeComponent;
import components.SpriteRenderer;
import org.joml.Vector4f;
import render.texture.Sprite;
import render.texture.SpriteSheet;
import utility.AssetsPool;

public class Indicator extends SpriteRenderer implements NotSerializeComponent {
    private static final String PATH = "engine://assets/textures/indicator.png";

    private transient SpriteSheet sheet;
    private transient boolean isInitialized = false;
    private transient boolean active = false;

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
            setActive();
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

        if (!active) setActive();

        super.editorUpdate(dt);
    }

    @Override
    public void update(float dt) {
        if (active) setInactive();
    }

    // It shouldn't be edited in the properties windows too.
    // Override imgui to do nothing to prevent this
    @Override
    public void imgui() {}

    private void setInactive() {
        active = false;
        setColor(new Vector4f(0.0f));
    }

    private void setActive() {
        active = true;
        setColor(new Vector4f(1.0f, 1.0f, 1.0f, 0.8f));
    }
}

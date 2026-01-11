package editor.components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.Viewport;
import components.NotSerializeComponent;
import components.SpriteRenderer;
import org.joml.Vector4f;
import render.texture.Sprite;
import render.texture.TextureUnit;
import utility.AssetsPool;
import utility.Settings;

public class EditorObjectIndicator extends SpriteRenderer implements NotSerializeComponent {
    private static final String PATH = Settings.TexturePath.ObjectIndicator;

    private transient TextureUnit textureUnit;
    private transient boolean isInitialized = false;
    private transient boolean active = false;

    @Override
    protected void onEditorStartLogic() {
        initIndicator();
    }

    private void initIndicator() {
        if (isInitialized
                || gameObject == null
                || !gameObject.isSerialize()
                || !(gameObject instanceof GameObject2D)
        ) return;

        try {
            if (!AssetsPool.hasTextureUnit(PATH)) {
                AssetsPool.addTextureUnit(PATH,
                        new TextureUnit(AssetsPool.loadTexture(PATH), 12, 12)
                );
            }

            textureUnit = AssetsPool.loadTextureUnit(PATH);
            setActive();
            completeInit();
        } catch (Exception e) {
            System.err.println("Failed to initialize EditorObjectIndicator: " + e.getMessage());
        }
    }

    private void completeInit() {
        if (textureUnit == null) return;

        Sprite sprite = textureUnit.getSprite();
        if (sprite == null) return;
        setSprite(sprite);

        zIndex(Viewport.farZIndex);
        localTransform2D.relativeZIndex = false;

        isInitialized = true;
    }

    @Override
    public void editorUpdate(float dt) {
        if (gameObject == null || !gameObject.isSerialize()) return;

        if (textureUnit == null) {
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

    /**
     * Prevent editing indicator's properties.
     */
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

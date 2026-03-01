package editor.components;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.Viewport;
import components.IsNotSelectable;
import components.NotSerializeComponent;
import components.SpriteRenderer;
import org.joml.Vector4f;
import render.texture.Sprite;
import render.texture.TextureUnit;
import utility.AssetsPool;
import utility.Settings;

/**
 * EditorObjectIndicator is an indicator sprite mounted to 2D object in scenes if the object is serialized.
 * It allows selecting 2D object that does not contain visible or selectable element on screen.
 * <p>
 * The indicator components is only mounted and active outside runtime mode.
 */
public final class EditorObjectIndicator extends SpriteRenderer implements NotSerializeComponent {
    private static final String IndicatorPath = Settings.TexturePath.ObjectIndicator;
    private transient TextureUnit textureUnit;
    private transient boolean isInitialized = false;
    private transient boolean active = false;

    /**
     * Create a new {@link EditorObjectIndicator} component.
     */
    public EditorObjectIndicator() {
        String name = EditorObjectIndicator.class.getSimpleName();
        super(name);
        zIndex(Viewport.FarZIndex);
        localTransform2D.relativeZIndex = false;
    }

    @Override
    protected void onEditorStart() {
        initIndicator();
    }

    private void initIndicator() {
        if (isInitialized
                || gameObject == null
                || !gameObject.isSerialize()
                || !(gameObject instanceof GameObject2D)
        ) return;
        try {
            if (!AssetsPool.hasTextureUnit(IndicatorPath)) {
                AssetsPool.addTextureUnit(IndicatorPath,
                        new TextureUnit(AssetsPool.loadTexture(IndicatorPath), 12, 12)
                );
            }
            textureUnit = AssetsPool.getTextureUnit(IndicatorPath);
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
        sprite(sprite);
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

    /**
     * Add editor indicator to the given game object.
     * This requires object of type {@link GameObject2D} or its subclasses.
     * @param go the game object to receive the component
     */
    public static void add(GameObject go) {
        if (go == null || go.isRemoved() || !go.isSerialize()) return;
        if (go.getFirstComponent(IsNotSelectable.class) != null) return;
        go.removeComponents(EditorObjectIndicator.class);
        if (go instanceof GameObject2D go2D) go2D.addComponent(new EditorObjectIndicator());
    }

    private void setInactive() {
        active = false;
        color(new Vector4f(0.0f));
    }

    private void setActive() {
        active = true;
        color(new Vector4f(1.0f, 1.0f, 1.0f, 0.8f));
    }
}

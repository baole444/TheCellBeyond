package editor.components;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.Transform2D;
import TheCellBeyond.Viewport;
import components.Component2D;
import components.IsNotSelectable;
import components.NotSerializeComponent;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.commands.RectCommand;
import render.commands.RenderCommand;
import render.commands.TransformCommand;
import render.texture.Sprite;
import render.texture.TextureUnit;
import utility.AssetManager;
import utility.Settings;
import utility.WorldUnit;

/**
 * EditorObjectIndicator is an indicator sprite mounted to 2D object in scenes if the object is serialized.
 * It allows selecting 2D object that does not contain visible or selectable element on screen.
 * <p>
 * The indicator components is only mounted and active outside runtime mode.
 */
public final class EditorObjectIndicator extends Component2D implements NotSerializeComponent {
    private static final String IndicatorPath = Settings.TexturePath.ObjectIndicator;
    private static final int VerticesPerQuad = 4;
    private transient TextureUnit textureUnit;
    private transient Sprite sprite;
    private transient final Vector4f color = new Vector4f();
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
    protected void onTransformDirty() {
        renderDirty = true;
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
            AssetManager manager = AssetManager.get();
            if (!manager.hasTextureUnit(IndicatorPath)) {
                manager.addTextureUnit(IndicatorPath,
                        new TextureUnit(manager.getTexture(manager.loadTexture(IndicatorPath)), 12, 12)
                );
            }
            textureUnit = manager.getTextureUnit(IndicatorPath);
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
        this.sprite = sprite;
        renderDirty = true;
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
        color.zero();
        renderDirty = true;
    }

    private void setActive() {
        active = true;
        color.set(1.0f, 1.0f, 1.0f, 0.8f);
        renderDirty = true;
    }

    @Override
    public boolean nonRepeatable() {
        return true;
    }

    @Override
    public TransformCommand buildTransformCommand() {
        Transform2D effectiveTransform = effectiveTransform();
        TransformCommand transform = TransformCommand.acquire();
        transform.position.set(effectiveTransform.position);
        transform.zIndex = effectiveTransform.zIndex;
        transform.markChanged();
        return transform;
    }

    @Override
    public RenderCommand buildRenderCommand() {
        RectCommand rect = RectCommand.acquire();
        rect.submitterID = gameObject != null ? gameObject.getUID() : 0;
        rect.modulate.set(color);
        if (sprite != null) {
            rect.size.set(WorldUnit.pixelToWorld(sprite.getWidth(), sprite.getHeight()));
            if (sprite.textureRID() != null) {
                rect.textureRID = sprite.textureRID();
                Vector2f[] uv = sprite.getTextureCoordinates();
                if (uv != null) for (int i = 0; i < VerticesPerQuad; i++) rect.uvCoordinates[i].set(uv[i]);
            }
        }
        rect.markChanged();
        return rect;
    }
}

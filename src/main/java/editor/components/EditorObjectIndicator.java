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
    private static final int IndicatorSize = 12;
    private static final int VerticesPerQuad = 4;
    private static Sprite indicatorSprite;
    private transient Sprite sprite;
    private transient final Vector4f color = new Vector4f();
    private transient boolean initialized = false;
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
    protected void internalEditorStart() {
        super.internalEditorStart();
        initIndicator();
    }

    private void initIndicator() {
        if (initialized || gameObject == null || !gameObject.isSerialize() || !(gameObject instanceof GameObject2D)) return;
        if (indicatorSprite == null) {
            Sprite crosshair = new Sprite();
            crosshair.texture(IndicatorPath);
            crosshair.width(IndicatorSize);
            crosshair.height(IndicatorSize);
            indicatorSprite = crosshair;
        }
        sprite = indicatorSprite;
        activate();
        initialized = true;
    }

    @Override
    protected void internalEditorUpdate(float dt) {
        if (gameObject == null || !gameObject.isSerialize()) return;
        if (!initialized) {
            initIndicator();
            return;
        }
        if (!active) activate();
    }

    @Override
    protected void internalUpdate(float dt) {
        if (active) deactivate();
    }

    /**
     * Add editor indicator to the given game object.
     * This requires object of type {@link GameObject2D} or its subclasses.
     * @param go the game object to receive the component
     */
    public static void add(GameObject go) {
        if (go == null || go.isDestroyed() || !go.isSerialize()) return;
        if (go.getFirstComponent(IsNotSelectable.class) != null) return;
        go.removeComponents(EditorObjectIndicator.class);
        if (go instanceof GameObject2D go2D) go2D.addComponent(new EditorObjectIndicator());
    }

    private void deactivate() {
        active = false;
        color.zero();
        renderDirty = true;
    }

    private void activate() {
        active = true;
        color.set(1.0f, 1.0f, 1.0f, 0.8f);
        renderDirty = true;
    }

    @Override
    public boolean nonRepeatable() {
        return true;
    }

    @Override
    public void syncTransform(TransformCommand current, TransformCommand previous) {
        Transform2D global = effectiveTransform();
        current.position.set(global.position);
        current.zIndex = global.zIndex;
        current.rotationDegrees = 0.0f;
        current.scale.set(1.0f);
        current.markChanged();
        previous.copyFrom(current);
    }

    @Override
    public RenderCommand buildRenderCommand() {
        RectCommand rect = RectCommand.acquire();
        rect.submitterID = gameObject != null ? gameObject.getUID() : 0;
        rect.modulate.set(color);
        if (sprite != null) {
            rect.size.set(WorldUnit.pixelToWorld(sprite.width(), sprite.height()));
            if (sprite.textureRID() != null) {
                rect.textureRID = sprite.textureRID();
                Vector2f[] uv = sprite.textureCoordinates();
                if (uv != null) for (int i = 0; i < VerticesPerQuad; i++) rect.uvCoordinates[i].set(uv[i]);
            }
        }
        rect.markChanged();
        return rect;
    }
}

package components;

import TheCellBeyond.*;
import editor.Properties;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.texture.Sprite;
import utility.WorldUnit;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A class dedicated to handle gizmo selection and activity, so as its position and appearance.
 * For handling gizmo's type and movement, see {@link GizmoControl}.
 */
public class Gizmo extends SpatialComponent implements NotSerializeComponent {
    private final Vector4f resetColor = new Vector4f(0, 0, 0 , 0);
    private final Vector4f xAxisColor = new Vector4f(0.7f, 0.2f, 0.2f, 1.0f);
    private final Vector4f xHover = new Vector4f(0.85f, 0.35f, 0.35f, 1.0f);
    private final Vector4f yAxisColor = new Vector4f(0.2f, 0.7f, 0.2f, 1.0f);
    private final Vector4f yHover = new Vector4f(0.35f, 0.85f, 0.35f, 1.0f);

    private final float offset = WorldUnit.pixelToWorld(34.0f);
    private final Vector2f xOffset = new Vector2f(offset, 0f);
    private final Vector2f yOffset = new Vector2f(0.0f, offset);

    private final float gizWidth = 0.16f;
    private final float gizHeight = 0.48f;

    protected boolean xActiveDrag = false;
    protected boolean yActiveDrag = false;
    private boolean isUsed = false;
    protected GameObject activeGameObj = null;

    private final GameObject2D xAxisObj;
    private final GameObject2D yAxisObj;
    private final SpriteRenderer xAxisSpr;
    private final SpriteRenderer yAxisSpr;
    private final Properties properties;

    private transient final Vector2f gizmoWorldPos = new Vector2f();

    // Create Gizmo, position, and color.
    // Mark Gizmo arrow is not a selectable object.
    // Push gizmo to the scene.
    public Gizmo(String type, Sprite arrowSprite, Properties properties) {
        xAxisObj = createGizmoObject(type + "gizmoX", arrowSprite);
        yAxisObj = createGizmoObject(type + "gizmoY", arrowSprite);
        xAxisObj.rotate(90);
        yAxisObj.rotate(180);
        xAxisSpr = xAxisObj.getFirstComponent(SpriteRenderer.class);
        yAxisSpr = yAxisObj.getFirstComponent(SpriteRenderer.class);
        this.properties = properties;

        Window.getScene().queueForObjectAddition(xAxisObj);
        Window.getScene().queueForObjectAddition(yAxisObj);
    }

    private GameObject2D createGizmoObject(String name, Sprite sprite) {
        GameObject2D go2D = new GameObject2D(name);

        // Make gizmo not store to level save file.
        go2D.setNotSerialize();

        go2D.addComponent(new IsNotSelectable());

        SpriteRenderer renderer = new SpriteRenderer();
        renderer.setLocalzIndex(100);
        renderer.setSprite(sprite);

        go2D.addComponent(renderer);
        return go2D;
    }

    @Override
    public void update(float dt) {
        setInactive();
    }

    @Override
    public void editorUpdate(float dt) {
        if (!isUsed) return;

        // Update onscreen active object.
        activeGameObj = properties.getActiveGameObject();
        if (activeGameObj == null) {
            setInactive();
            return;
        }

        if (activeGameObj.getFirstComponent(IsNotSelectable.class) != null) {
            activeGameObj = null;
            setInactive();
            return;
        }

        setActive();

        updatePosition();
        handleInteraction();
    }

    private void updatePosition() {
        if (activeGameObj == null) return;

        if (!(activeGameObj instanceof GameObject2D go2D)) {
            setInactive();
            return;
        }

        Vector2f targetPos = go2D.getPosition();
        setWorldPosition(targetPos);

        gizmoWorldPos.set(getWorldPosition());

        xAxisObj.setPosition(new Vector2f(gizmoWorldPos).add(xOffset));
        yAxisObj.setPosition(new Vector2f(gizmoWorldPos).add(yOffset));
    }

    private void handleInteraction() {
        boolean xAxisHover = isHoverX();
        boolean yAxisHover = isHoverY();

        if ((xAxisHover || xActiveDrag) && MouseListener.isDragging() && MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT)) {
            xActiveDrag = true;
            yActiveDrag = false;
        } else if ((yAxisHover || yActiveDrag) && MouseListener.isDragging() && MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT)) {
            yActiveDrag = true;
            xActiveDrag = false;
        } else {
            xActiveDrag = false;
            yActiveDrag = false;
        }
    }

    private void setActive() {
        xAxisSpr.setColor(xAxisColor);
        yAxisSpr.setColor(yAxisColor);
    }

    private void setInactive() {
        xAxisSpr.setColor(resetColor);
        yAxisSpr.setColor(resetColor);
    }

    private boolean isHoverX() {
        Vector2f cursorPos = MouseListener.getWorld();
        Vector2f xAxisPos = this.xAxisObj.getPosition();
        if (cursorPos.x <= xAxisPos.x + (gizHeight / 2.0f) &&
                cursorPos.x >= xAxisPos.x - (gizWidth / 2.0f) &&
                cursorPos.y >= xAxisPos.y - (gizHeight / 2.0f) &&
                cursorPos.y <= xAxisPos.y + (gizWidth / 2.0f)
        ) {
            xAxisSpr.setColor(xHover);
            return true;
        }
        xAxisSpr.setColor(xAxisColor);
        return false;
    }

    private boolean isHoverY() {
        Vector2f cursorPos = MouseListener.getWorld();
        Vector2f yAxisPos = this.yAxisObj.getPosition();
        if (cursorPos.x <= yAxisPos.x + (gizWidth / 2.0f) &&
                cursorPos.x >= yAxisPos.x - (gizWidth / 2.0f) &&
                cursorPos.y <= yAxisPos.y + (gizHeight / 2.0f) &&
                cursorPos.y >= yAxisPos.y - (gizHeight / 2.0f)
        ) {
            yAxisSpr.setColor(yHover);
            return true;
        }
        yAxisSpr.setColor(yAxisColor);
        return false;
    }

    public void use() {
        isUsed = true;
        setActive();
    }

    public void stopUse() {
        isUsed = false;
        setInactive();
    }
}

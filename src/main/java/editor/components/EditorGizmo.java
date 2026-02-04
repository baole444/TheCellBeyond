package editor.components;

import TheCellBeyond.*;
import TheCellBeyond.internal.LogicServer;
import components.*;
import editor.Properties;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.texture.Sprite;
import utility.WorldUnit;

import static org.lwjgl.glfw.GLFW.*;

/**
 * EditorGizmo is an Editor's component, use to display moving arrows or scale arrows.
 * It will appear at the position of the editing game object (if the object exists in the spatial logic world).
 * @see EditorGizmoCtrl Handling gizmo's type and movement
 */
public class EditorGizmo extends SpatialComponent implements NotSerializeComponent {
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

    private transient final Vector2f gizmoWorldPos = new Vector2f();

    // Create EditorGizmo, position, and color.
    // Mark EditorGizmo arrow is not a selectable object.
    // Push gizmo to the scene.
    public EditorGizmo(String type, Sprite arrowSprite) {
        xAxisObj = createGizmoObject(type + "gizmoX", arrowSprite);
        yAxisObj = createGizmoObject(type + "gizmoY", arrowSprite);
        xAxisObj.rotate(90);
        yAxisObj.rotate(180);
        xAxisSpr = xAxisObj.getFirstComponent(SpriteRenderer.class);
        yAxisSpr = yAxisObj.getFirstComponent(SpriteRenderer.class);

        LogicServer.currentScene().queueForObjectAddition(xAxisObj);
        LogicServer.currentScene().queueForObjectAddition(yAxisObj);
    }

    private GameObject2D createGizmoObject(String name, Sprite sprite) {
        GameObject2D go2D = new GameObject2D(name);

        // Make gizmo not store to level save file.
        go2D.setNotSerialize();

        go2D.addComponent(new IsNotSelectable());

        SpriteRenderer renderer = new SpriteRenderer();
        renderer.zIndex(100);
        renderer.sprite(sprite);

        go2D.addComponent(renderer);
        return go2D;
    }

    @Override
    public void update(float dt) {
        setInactive();
    }

    /**
     * Sync the gizmo with the editing game object from {@link Properties}.
     * @param dt delta time
     */
    @Override
    public void editorUpdate(float dt) {
        if (!isUsed) return;

        activeGameObj = Properties.getActiveGameObject();
        if (activeGameObj == null || activeGameObj.isRemoved()) {
            activeGameObj = null;
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

        Vector2f targetPos = go2D.globalPosition();
        setWorldPosition(targetPos);

        gizmoWorldPos.set(getObjectWorldPosition());

        xAxisObj.position(new Vector2f(gizmoWorldPos).add(xOffset));
        yAxisObj.position(new Vector2f(gizmoWorldPos).add(yOffset));
    }

    private void handleInteraction() {
        boolean xAxisHover = isHoverX();
        boolean yAxisHover = isHoverY();

        if ((xAxisHover || xActiveDrag) && MouseListener.isDragging() && MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_RIGHT)) {
            xActiveDrag = true;
            yActiveDrag = false;
        } else if ((yAxisHover || yActiveDrag) && MouseListener.isDragging() && MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_RIGHT)) {
            yActiveDrag = true;
            xActiveDrag = false;
        } else {
            xActiveDrag = false;
            yActiveDrag = false;
        }
    }

    private void setActive() {
        xAxisSpr.color(xAxisColor);
        yAxisSpr.color(yAxisColor);
    }

    private void setInactive() {
        xAxisSpr.color(resetColor);
        yAxisSpr.color(resetColor);
    }

    private boolean isHoverX() {
        Vector2f cursorPos = MouseListener.getWorldPosition();
        Vector2f xAxisPos = this.xAxisObj.globalPosition();
        if (cursorPos.x <= xAxisPos.x + (gizHeight / 2.0f) &&
                cursorPos.x >= xAxisPos.x - (gizWidth / 2.0f) &&
                cursorPos.y >= xAxisPos.y - (gizHeight / 2.0f) &&
                cursorPos.y <= xAxisPos.y + (gizWidth / 2.0f)
        ) {
            xAxisSpr.color(xHover);
            return true;
        }
        xAxisSpr.color(xAxisColor);
        return false;
    }

    private boolean isHoverY() {
        Vector2f cursorPos = MouseListener.getWorldPosition();
        Vector2f yAxisPos = this.yAxisObj.globalPosition();
        if (cursorPos.x <= yAxisPos.x + (gizWidth / 2.0f) &&
                cursorPos.x >= yAxisPos.x - (gizWidth / 2.0f) &&
                cursorPos.y <= yAxisPos.y + (gizHeight / 2.0f) &&
                cursorPos.y >= yAxisPos.y - (gizHeight / 2.0f)
        ) {
            yAxisSpr.color(yHover);
            return true;
        }
        yAxisSpr.color(yAxisColor);
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

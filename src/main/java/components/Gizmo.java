package components;

import TCB_Field.*;
import editor.Properties;
import org.joml.Vector2f;
import org.joml.Vector4f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A class dedicated to handle gizmo selection and activity, so as its position and appearance.
 * For handling gizmo's type and movement, see {@link GizmoControl}.
 */
public class Gizmo extends Component {
    private final Vector4f resetColor = new Vector4f(0, 0, 0 , 0);
    private final Vector4f xAxisColor = new Vector4f(0.7f, 0.2f, 0.2f, 1.0f);
    private final Vector4f xHover = new Vector4f(0.85f, 0.35f, 0.35f, 1.0f);
    private final Vector4f yAxisColor = new Vector4f(0.2f, 0.7f, 0.2f, 1.0f);
    private final Vector4f yHover = new Vector4f(0.35f, 0.85f, 0.35f, 1.0f);
    private final Vector2f xOffset = new Vector2f(34.0f / 100, 0f / 100);
    private final Vector2f yOffset = new Vector2f(0.0f / 100, 34.0f / 100);
    private float gizWidth = 0.16f;
    private float gizHeight = 0.48f;
    protected boolean xActiveDrag = false;
    protected boolean yActiveDrag = false;
    private boolean isUsed = false;
    protected GameObject activeGameObj = null;


    private GameObject xAxisObj;
    private GameObject yAxisObj;
    private SpriteRender xAxisSpr;
    private SpriteRender yAxisSpr;
    private Properties properties;

    // Create Gizmo, give it size, position, and color.
    // Mark Gizmo arrow is not a selectable object.
    // Push gizmo to the scene.
    public Gizmo(Sprite arrowSprite, Properties properties) {
        this.xAxisObj = Prefab.genSpsObj(arrowSprite, gizWidth, gizHeight);
        this.yAxisObj = Prefab.genSpsObj(arrowSprite, gizWidth, gizHeight);
        this.xAxisSpr = this.xAxisObj.getComponent(SpriteRender.class);
        this.yAxisSpr = this.yAxisObj.getComponent(SpriteRender.class);
        this.properties = properties;

        this.xAxisObj.addComponent(new IsNotSelectable());
        this.yAxisObj.addComponent(new IsNotSelectable());

        Window.getScene().addObjToScene(this.xAxisObj);
        Window.getScene().addObjToScene(this.yAxisObj);

    }

    @Override
    public void start() {
        // Give gizmo correct rotation direction.
        this.xAxisObj.transform.rotate = 90;
        this.yAxisObj.transform.rotate = 180;

        // Push gizmo index to high value in order for the texture to be above the item.
        this.xAxisObj.transform.zIndex = 100;
        this.yAxisObj.transform.zIndex = 100;

        // Make gizmo not store to level save file.
        this.xAxisObj.isNotSerialize();
        this.yAxisObj.isNotSerialize();
    }

    @Override
    public void update(float dt) {
        if (isUsed) {
            this.setInactiveObj();
        }
        this.setInactiveObj();
    }

    // TODO: Move this to its own "keyCtrl" class similar to MouseCtrl
    @Override
    public void editorUpdate(float dt) {
        if (!isUsed) return;

        // Update onscreen active object.
        this.activeGameObj = this.properties.loadActiveObj();
        if (this.activeGameObj != null) {
            this.setActiveObj();
        } else {
            this.setInactiveObj();
            return;
        }

        // Update if gizmo is being hovered.
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

        if (this.activeGameObj != null) {
            this.xAxisObj.transform.position.set(this.activeGameObj.transform.position);
            this.yAxisObj.transform.position.set(this.activeGameObj.transform.position);
            this.xAxisObj.transform.position.add(this.xOffset);
            this.yAxisObj.transform.position.add(this.yOffset);
        }
    }

    private void setActiveObj() {
        this.xAxisSpr.setColor(xAxisColor);
        this.yAxisSpr.setColor(yAxisColor);
    }

    private void setInactiveObj() {
        this.xAxisSpr.setColor(resetColor);
        this.yAxisSpr.setColor(resetColor);
    }

    private boolean isHoverX() {
        Vector2f cursorPos = MouseListener.getWorld();
        if (cursorPos.x <= xAxisObj.transform.position.x + (gizHeight / 2.0f) &&
                cursorPos.x >= xAxisObj.transform.position.x - (gizWidth / 2.0f) &&
                cursorPos.y >= xAxisObj.transform.position.y - (gizHeight / 2.0f) &&
                cursorPos.y <= xAxisObj.transform.position.y + (gizWidth / 2.0f)
        ) {
            xAxisSpr.setColor(xHover);
            return true;
        }
        xAxisSpr.setColor(xAxisColor);
        return false;
    }

    private boolean isHoverY() {
        Vector2f cursorPos = MouseListener.getWorld();
        if (cursorPos.x <= yAxisObj.transform.position.x + (gizWidth / 2.0f) &&
                cursorPos.x >= yAxisObj.transform.position.x - (gizWidth / 2.0f) &&
                cursorPos.y <= yAxisObj.transform.position.y + (gizHeight / 2.0f) &&
                cursorPos.y >= yAxisObj.transform.position.y - (gizHeight / 2.0f)
        ) {
            yAxisSpr.setColor(yHover);
            return true;
        }
        yAxisSpr.setColor(yAxisColor);
        return false;
    }

    public void setUse() {
        this.isUsed = true;
    }

    public void setUnUse() {
        this.isUsed = false;
        this.setInactiveObj();
    }
}

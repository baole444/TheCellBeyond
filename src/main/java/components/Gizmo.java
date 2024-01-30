package components;

import TCB_Field.GameObject;
import TCB_Field.MouseListener;
import TCB_Field.Prefab;
import TCB_Field.Window;
import editor.Properties;
import org.joml.Vector2f;
import org.joml.Vector4f;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class Gizmo extends Component {
    private final Vector4f resetColor = new Vector4f(0, 0, 0 , 0);
    private final Vector4f xAxisColor = new Vector4f(0.7f, 0.2f, 0.2f, 1.0f);
    private final Vector4f xHover = new Vector4f(0.85f, 0.35f, 0.35f, 1.0f);
    private final Vector4f yAxisColor = new Vector4f(0.2f, 0.7f, 0.2f, 1.0f);
    private final Vector4f yHover = new Vector4f(0.35f, 0.85f, 0.35f, 1.0f);
    private final Vector2f xOffset = new Vector2f(63.0f, 8f);
    private final Vector2f yOffset = new Vector2f(24.0f, 63.0f);
    private int gizWidth = 16;
    private int gizHeight = 48;
    protected boolean xActiveDrag = false;
    protected boolean yActiveDrag = false;
    private boolean isUsed = false;
    protected GameObject activeGameObj = null;


    private GameObject xAxisObj;
    private GameObject yAxisObj;
    private SpriteRender xAxisSpr;
    private SpriteRender yAxisSpr;
    private Properties properties;

    public Gizmo(Sprite arrowSprite, Properties properties) {
        this.xAxisObj = Prefab.genSpsObj(arrowSprite, 16, 48);
        this.yAxisObj = Prefab.genSpsObj(arrowSprite, 16, 48);
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
        this.xAxisObj.transform.rotate = 90;
        this.yAxisObj.transform.rotate = 180;
        this.xAxisObj.transform.zIndex = 100;
        this.yAxisObj.transform.zIndex = 100;
        this.xAxisObj.isNotSerialize();
        this.yAxisObj.isNotSerialize();
    }

    @Override
    public void update(float dt) {
        if (!isUsed) return;

        this.activeGameObj = this.properties.loadActiveObj();
        if (this.activeGameObj != null) {
            this.setActiveObj();
        } else {
            this.setInactiveObj();
            return;
        }

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
        this.activeGameObj = null;
        this.xAxisSpr.setColor(resetColor);
        this.yAxisSpr.setColor(resetColor);
    }

    private boolean isHoverX() {
        Vector2f cursorPos = new Vector2f(MouseListener.getOrthoX(), MouseListener.getOrthoY());
        if (cursorPos.x <= xAxisObj.transform.position.x &&
                cursorPos.x >= xAxisObj.transform.position.x - gizHeight &&
                cursorPos.y >= xAxisObj.transform.position.y &&
                cursorPos.y <= xAxisObj.transform.position.y + gizWidth
        ) {
            xAxisSpr.setColor(xHover);
            return true;
        }
        xAxisSpr.setColor(xAxisColor);
        return false;
    }

    private boolean isHoverY() {
        Vector2f cursorPos = new Vector2f(MouseListener.getOrthoX(), MouseListener.getOrthoY());
        if (cursorPos.x <= yAxisObj.transform.position.x &&
                cursorPos.x >= yAxisObj.transform.position.x - gizWidth &&
                cursorPos.y <= yAxisObj.transform.position.y &&
                cursorPos.y >= yAxisObj.transform.position.y - gizHeight
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

package TCB_Field;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class MouseListener {
    private  static MouseListener instance;
    private double scrollX, scrollY;
    private double xPos, yPos, lastY, lastX;
    private boolean mouseButtonPressed[] = new boolean[3];
    private boolean isDragging;

    private Vector2f workViewportPos = new Vector2f();
    private Vector2f workViewportSize = new Vector2f();

    private MouseListener() {
        this.scrollX = 0.0;
        this.scrollY = 0.0;
        this.xPos = 0.0;
        this.yPos = 0.0;
        this.lastX = 0.0;
        this.lastY = 0.0;

    }

    public static MouseListener get() {
        if (MouseListener.instance == null) {
            instance = new MouseListener();
        }

        return MouseListener.instance;
    }

    public static void mousePosCallback(long window, double xpos, double ypos) {
        get().lastX = get().xPos;
        get().lastY = get().yPos;
        get().xPos  = xpos;
        get().yPos = ypos;
        get().isDragging = get().mouseButtonPressed[0] || get().mouseButtonPressed[1] || get().mouseButtonPressed[2];
    }

    public static void mouseButtonCallback(long window, int button, int action, int mods) {
        if (action == GLFW_PRESS) {
            if (button < get().mouseButtonPressed.length) {
                get().mouseButtonPressed[button] = true;
            }
        } else if (action == GLFW_RELEASE) {
            if (button < get().mouseButtonPressed.length) {
                get().mouseButtonPressed[button] = false;
                get().isDragging = false;
            }
        }
    }

    public static void mouseScrollCallback(long window, double xOffset, double yOffset) {
        get().scrollX = xOffset;
        get().scrollY = yOffset;
    }

    public static void endFrame() {
        get().scrollX = 0;
        get().scrollY = 0;
        get().lastX = get().xPos;
        get().lastY = get().yPos;
    }

    public static float getX() {
        return (float)get().xPos;
    }

    public static float getY() {
        return (float)get().yPos;
    }

    public static float getDX() {
        return (float)(get().lastX - get().xPos);
    }

    public static float getDY() {
        return (float) (get().lastY - get().yPos);
    }

    public static float getScrollX() {
        return (float)get().scrollX;
    }

    public static float getScrollY() {
        return (float)get().scrollY;
    }

    public static boolean isDragging() {
        return get().isDragging;
    }

    public static  boolean mouseButtonDown(int button) {
        if(button < get().mouseButtonPressed.length) {
            return get().mouseButtonPressed[button];
        } else {
            return false;
        }
    }

    public static float getOrthoX() {
        float instX = getX() - get().workViewportPos.x;
        instX = (instX / get().workViewportSize.x) * 2.0f - 1.0f;
        Vector4f tmp = new Vector4f(instX, 0, 0, 1);

        Viewport vp = Window.getScene().viewport();
        Matrix4f viewProject = new Matrix4f();
        vp.getInverseProject().mul(vp.getInverseView(), viewProject);

        tmp.mul(viewProject);

        instX = tmp.x;

        System.out.println(instX);

        return instX;
    }

    public static float getOrthoY() {
        float instY = getY() - get().workViewportPos.y;
        instY = -((instY / get().workViewportSize.y) * 2.0f - 1.0f);
        Vector4f tmp = new Vector4f(0, instY, 0, 1);

        Viewport vp = Window.getScene().viewport();
        Matrix4f viewProject = new Matrix4f();
        vp.getInverseProject().mul(vp.getInverseView(), viewProject);

        tmp.mul(viewProject);

        instY = tmp.y;

        return instY;
    }

    public static void setWorkViewportPos(Vector2f workViewportPos) {
        get().workViewportPos.set(workViewportPos);
    }

    public static void setWorkViewportSize(Vector2f workViewportSize) {
        get().workViewportSize.set(workViewportSize);
    }
}

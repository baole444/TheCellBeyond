package TCB_Field;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.ObjectSelection;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class MouseListener {
    private  static MouseListener instance;
    private double scrollX, scrollY;
    private double xPos, yPos,  xWorld, yWorld, xLast, yLast, xWorldLast, yWorldLast;
    private boolean mouseButtonPressed[] = new boolean[9];
    private boolean isDragging;
    private int mouseButtonDown = 0;
    private Vector2f workViewportPos = new Vector2f();
    private Vector2f workViewportSize = new Vector2f();
    private ObjectSelection objectSelection;

    private MouseListener() {
        this.scrollX = 0.0;
        this.scrollY = 0.0;
        this.xPos = 0.0;
        this.yPos = 0.0;
        this.xLast = 0.0;
        this.yLast = 0.0;
    }

    public static void endFrame() {
        get().scrollX = 0;
        get().scrollY = 0;
    }

    public static void clear() {
        get().scrollX = 0.0;
        get().scrollY = 0.0;
        get().xPos = 0.0;
        get().yPos = 0.0;
        get().xLast = 0.0;
        get().yLast = 0.0;
        get().mouseButtonDown = 0;
        get().isDragging = false;
        for (int i = 0; i < get().mouseButtonPressed.length; i++) {
            get().mouseButtonPressed[i] = false;
        }
    }

    public static MouseListener get() {
        if (MouseListener.instance == null) {
            instance = new MouseListener();
        }

        return MouseListener.instance;
    }

    public static void mousePosCallback(long window, double xpos, double ypos) {
        if (!Window.loadImGui().loadGameViewPort().getWantCaptureMouse()) {
            clear();
        }

        if (get().mouseButtonDown > 0) {
            get().isDragging = true;
        }

        get().xLast = get().xPos;
        get().yLast = get().yPos;
        get().xWorldLast = get().xWorld;
        get().yWorldLast = get().yWorld;
        get().xPos  = xpos;
        get().yPos = ypos;
    }

    public static void mouseButtonCallback(long window, int button, int action, int mods) {
        if (action == GLFW_PRESS) {
            get().mouseButtonDown++;

            if (button < get().mouseButtonPressed.length) {
                get().mouseButtonPressed[button] = true;
            }
        } else if (action == GLFW_RELEASE) {
            get().mouseButtonDown--;

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

    public static float getX() {
        return (float)get().xPos;
    }

    public static float getY() {
        return (float)get().yPos;
    }

    public static float getScrollX() {
        return (float)get().scrollX;
    }

    public static float getScrollY() {
        return (float)get().scrollY;
    }

    public static float getWorldDX() {
        return (float)(get().xWorldLast - get().xPos);
    }

    public static float getWorldDY() {
        return (float)(get().yWorldLast - get().yPos);
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



    // Remove the need to recalculate mouse callback each time it is call in a same frame
    public static float getWorldX() {
        return getWorld().x;
    }

    public static float getWorldY() {
        return getWorld().y;
    }

    public static Vector2f getWorld() {
        float  instX = getX() - get().workViewportPos.x;
        instX = (2.0f * (instX / get().workViewportSize.x)) - 1.0f;
        float instY = (getY() - get().workViewportPos.y);
        instY = (2.0f * (1.0f - (instY / get().workViewportSize.y))) - 1.0f;

        Viewport viewport = Window.getScene().viewport();
        Vector4f tmp = new Vector4f(instX, instY, 0, 1);
        Matrix4f inverseView = new Matrix4f(viewport.getInverseView());
        Matrix4f inverseProjection = new Matrix4f(viewport.getInverseProject());
        tmp.mul(inverseView.mul(inverseProjection));

        return new Vector2f(tmp.x, tmp.y);
    }

    public static float loadScrX() {
        return loadScr().x;
    }
    public static float loadScrY() {
        return loadScr().y;
    }

    public static Vector2f loadScr() {
        float instX = getX() - get().workViewportPos.x;
        instX = (instX / get().workViewportSize.x) * Window.loadWidth();

        float instY = getY() - get().workViewportPos.y;
        instY = (1.0f - (instY / get().workViewportSize.y)) * Window.loadHeight();

        return new Vector2f(instX, instY);
    }

    public static Vector2f scr2World(Vector2f scCoord) {
        Vector2f normalizedScrCoords = new Vector2f(
                scCoord.x / Window.loadWidth(),
                scCoord.y / Window.loadHeight()
        );
        normalizedScrCoords.mul(2.0f).sub(new Vector2f(1.0f, 1.0f));

        Viewport vp = Window.getScene().viewport();
        Vector4f tmp = new Vector4f(normalizedScrCoords.x, normalizedScrCoords.y, 0, 1);

        Matrix4f inverseView = new Matrix4f(vp.getInverseView());
        Matrix4f inverseProject = new Matrix4f(vp.getInverseProject());
        tmp.mul(inverseView.mul(inverseProject));

        return new Vector2f(tmp.x, tmp.y);
    }

    public static Vector2f world2Scr(Vector2f worldCoords) {
        Viewport vp = Window.getScene().viewport();

        Vector4f iwSpacePos = new Vector4f(worldCoords.x, worldCoords.y, 0 , 1);
        Matrix4f view = new Matrix4f(vp.getViewMatrix());
        Matrix4f project = new Matrix4f(vp.getProjectMatrix());

        iwSpacePos.mul(project.mul(view));

        Vector2f winSpace = new Vector2f(iwSpacePos.x, iwSpacePos.y).mul(1.0f / iwSpacePos.w);

        winSpace.add(new Vector2f(1.0f, 1.0f)).mul(0.5f);
        winSpace.mul(new Vector2f(Window.loadWidth(), Window.loadHeight()));

        return winSpace;
    }
    //--------------------------------------------------------------------

    public static void setWorkViewportPos(Vector2f workViewportPos) {
        get().workViewportPos.set(workViewportPos);
    }

    public static void setWorkViewportSize(Vector2f workViewportSize) {
        get().workViewportSize.set(workViewportSize);
    }

}

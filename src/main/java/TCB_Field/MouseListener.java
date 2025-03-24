package TCB_Field;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.ObjectSelection;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class MouseListener {
    private static MouseListener instance;
    private double scrollX, scrollY;
    private double xPos, yPos, worldPastX, worldPastY, worldCurrentX, worldCurrentY;
    private final boolean[] mouseButtonPressed = new boolean[3];
    private boolean isDragging;
    private int mouseButtonDown = 0;
    private final Vector2f workViewportPos = new Vector2f();
    private final Vector2f workViewportSize = new Vector2f();
    private ObjectSelection objectSelection;

    private MouseListener() {
        this.scrollX = 0.0;
        this.scrollY = 0.0;
        this.xPos = 0.0;
        this.yPos = 0.0;
    }

    public static MouseListener get() {
        if (MouseListener.instance == null) {
            instance = new MouseListener();
        }

        return MouseListener.instance;
    }

    public static void mousePosCallback(long window, double xpos, double ypos) {
        if (!Window.loadImGui().getGameViewPort().getWantCaptureMouse()) {
            clear();
        }

        if (get().mouseButtonDown > 0) {
            get().isDragging = true;
        }

        get().xPos  = xpos;
        get().yPos = ypos;
        get().worldPastX = get().worldCurrentX;
        get().worldPastY = get().worldCurrentY;
        getWorldX();
        getWorldY();
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

    public static void endFrame() {
        get().scrollX = 0;
        get().scrollY = 0;
        get().worldPastX = get().worldCurrentX;
        get().worldPastY = get().worldCurrentY;
    }

    public static void clear() {
        get().scrollX = 0.0;
        get().scrollY = 0.0;
        get().xPos = 0.0;
        get().yPos = 0.0;
        get().mouseButtonDown = 0;
        get().isDragging = false;
        Arrays.fill(get().mouseButtonPressed, false);
    }

    public static Vector2f getCursorTraverse() {
        return new Vector2f(
                (float)(get().worldPastX - MouseListener.getWorldX()),
                (float)(get().worldPastY - MouseListener.getWorldY())
        );
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

    public static boolean isDragging() {
        return get().isDragging;
    }

    public static boolean mouseButtonDown(int button) {
        if(button < get().mouseButtonPressed.length) {
            return get().mouseButtonPressed[button];
        } else {
            return false;
        }
    }

    public static float getScreenX() {
        return getScreen().x;
    }
    public static float getScreenY() {
        return getScreen().y;
    }

    public static Vector2f getScreen() {
        float instX = getX() - get().workViewportPos.x;
        instX = (instX / get().workViewportSize.x) * 1920.0f;

        float instY = getY() - get().workViewportPos.y;
        instY = 1080.0f - ((instY / get().workViewportSize.y) * 1080.0f);

        return new Vector2f(instX, instY);
    }

    public static void setWorkViewportPos(Vector2f workViewportPos) {
        get().workViewportPos.set(workViewportPos);
    }

    public static void setWorkViewportSize(Vector2f workViewportSize) {
        get().workViewportSize.set(workViewportSize);
    }

    // Remove the need to recalculate mouse callback each time it is call in a same frame
    public static float getWorldX() {
        return getWorld().x;
    }

    public static float getWorldY() {
        return getWorld().y;
    }

    // raw mouse coordinate to world normalization coordinate
    public static Vector2f getWorld() {
        float currentX = getX() - get().workViewportPos.x;
        currentX = (2.0f * (currentX / get().workViewportSize.x)) - 1.0f;
        float currentY = (getY() - get().workViewportPos.y);
        currentY = (2.0f * (1.0f - (currentY / get().workViewportSize.y))) - 1;

        Viewport camera = Window.getScene().viewport();

        Vector4f tmp = new Vector4f(currentX, currentY, 0, 1);

        Matrix4f inverseView = new Matrix4f(camera.getInverseView());
        Matrix4f inverseProjection = new Matrix4f(camera.getInverseProject());

        tmp.mul(inverseView.mul(inverseProjection));

        // reserved for traverse calculation
        get().worldCurrentX = tmp.x;
        get().worldCurrentY = tmp.y;

        return new Vector2f(tmp.x, tmp.y);
    }
    //--------------------------------------------------------------------
    // Screen Coordinate = P * V * M
    // World Coordinate = S * V^-1 * p^-1


    public static Vector2f screen2WorldCoord(Vector2f scrCoord) {
        Vector2f normalization = new Vector2f(
                scrCoord.x / Window.loadWidth(),
                scrCoord.y / Window.loadHeight()
        );
        // Shift coordinates range back to -1 > 1
        normalization.mul(2f).sub(new Vector2f(1f, 1f));

        Viewport viewport = Window.getScene().viewport();

        Vector4f tmp = new Vector4f(normalization.x, normalization.y, 0 , 1);

        Matrix4f inverseView = new Matrix4f(viewport.getInverseView());
        Matrix4f inverseProjection = new Matrix4f(viewport.getInverseProject());

        tmp.mul(inverseView.mul(inverseProjection));

        return new Vector2f(tmp.x, tmp.y);
    }

    public static Vector2f world2ScreenCoord(Vector2f wCoord) {
        Viewport viewport = Window.getScene().viewport();

        Vector4f normalization = new Vector4f(wCoord.x, wCoord.y, 0, 1);

        Matrix4f view = new Matrix4f(viewport.getViewMatrix());
        Matrix4f projection = new Matrix4f(viewport.getProjectMatrix());

        normalization.mul(projection.mul(view));

        Vector2f windowSpace = new Vector2f(normalization.x, normalization.y).
                mul(1f / normalization.w);

        windowSpace.add(new Vector2f(1f, 1f)).mul(0.5f);
        windowSpace.mul(new Vector2f(Window.loadWidth(), Window.loadHeight()));

        return windowSpace;
    }
}

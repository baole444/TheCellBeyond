package TheCellBeyond;

import TheCellBeyond.internal.LogicServer;
import editor.ImGuiLayer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class MouseListener {
    private static MouseListener instance;

    private double scrollX, scrollY;
    private double xPos, yPos, worldPastX, worldPastY, worldCurrentX, worldCurrentY;

    private final boolean[] buttonPressed = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private final boolean[] buttonReleased = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    private boolean isDragging;
    private int countButtonDown = 0;

    private final Vector2f currentViewportPosition = new Vector2f();
    private final Vector2f currentViewportSize = new Vector2f();

    private static boolean startupMode = true;
    private final List<Integer> pressedKeyCodes = new ArrayList<>();

    private MouseListener() {
        scrollX = 0.0;
        scrollY = 0.0;
        xPos = 0.0;
        yPos = 0.0;
    }

    public static void setStartupMode(boolean mode) {
        startupMode = mode;
    }

    public static synchronized MouseListener get() {
        if (MouseListener.instance == null) {
            instance = new MouseListener();
        }

        return MouseListener.instance;
    }

    public static synchronized void mousePosCallback(long window, double xPos, double yPos) {
        ImGuiLayer layer = Window.getImGuiLayer();
        if (!startupMode && layer != null
                && layer.getSceneEditorViewPort() != null
                && !layer.getSceneEditorViewPort().getWantCaptureMouse()) {
            clear();
        }

        MouseListener listener = get();
        if (listener == null) return;

        if (listener.countButtonDown > 0) {
            listener.isDragging = true;
        }

        listener.xPos = xPos;
        listener.yPos = yPos;

        listener.worldPastX = listener.worldCurrentX;
        listener.worldPastY = listener.worldCurrentY;
    }

    public static void mouseButtonCallback(long window, int button, int action, int mods) {
        MouseListener listener = get();
        if (listener == null) return;

        if (action == GLFW_PRESS) {
            listener.countButtonDown++;
            if (isKeyValid(button)) {
                listener.buttonPressed[button] = true;
                listener.buttonReleased[button] = false;
                listener.pressedKeyCodes.add(button);
            }
            return;
        }

        if (action == GLFW_RELEASE) {
            listener.countButtonDown--;

            if (isKeyValid(button)) {
                listener.buttonPressed[button] = false;
                listener.buttonReleased[button] = true;
                listener.isDragging = false;
            }
        }
    }

    public static void mouseScrollCallback(long window, double xOffset, double yOffset) {
        get().scrollX = xOffset;
        get().scrollY = yOffset;
    }

    public static void endFrame() {
        MouseListener listener = get();
        if (listener == null) return;

        listener.scrollX = 0;
        listener.scrollY = 0;

        Arrays.fill(listener.buttonReleased, false);
        listener.pressedKeyCodes.clear();
        if (!startupMode && LogicServer.currentScene() != null) {
            listener.worldPastX = listener.worldCurrentX;
            listener.worldPastY = listener.worldCurrentY;
        }
    }

    public static void clear() {
        MouseListener listener = get();
        if (listener == null) return;

        listener.scrollX = 0.0;
        listener.scrollY = 0.0;
        listener.xPos = 0.0;
        listener.yPos = 0.0;
        listener.countButtonDown = 0;
        listener.isDragging = false;
        Arrays.fill(listener.buttonPressed, false);
        Arrays.fill(listener.buttonReleased, false);
        listener.pressedKeyCodes.clear();
    }

    public static List<Integer> getPressedButtons() {
        MouseListener listener = get();
        if (listener == null) return List.of();

        return new ArrayList<>(listener.pressedKeyCodes);
    }

    public static Vector2f getCursorWorldTraverse() {
        if (startupMode) return new Vector2f(0.0f);
        MouseListener listener = get();
        if (listener == null) return new Vector2f(0.0f);

        float x = (float) (listener.worldPastX - getWorldPositionX());
        float y = (float) (listener.worldPastY - getWorldPositionY());
        return new Vector2f(x, y);
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

    public static boolean isButtonPressed(int keyCode) {
        MouseListener listener = get();
        if (listener == null || !isKeyValid(keyCode)) return false;

        return listener.buttonPressed[keyCode];
    }

    public static boolean isButtonReleased(int keyCode) {
        MouseListener listener = get();
        if (listener == null || !isKeyValid(keyCode)) return false;

        return listener.buttonReleased[keyCode];
    }

    public static float getScreenPositionX() {
        return getScreenPosition().x;
    }
    public static float getScreenPositionY() {
        return getScreenPosition().y;
    }

    public static Vector2f getScreenPosition() {
        if (startupMode) return new Vector2f(getX(), getY());

        float instX = getX() - get().currentViewportPosition.x;
        instX = (instX / get().currentViewportSize.x) * Window.getWidth();

        float instY = getY() - get().currentViewportPosition.y;
        instY = Window.getHeight() - ((instY / get().currentViewportSize.y) * Window.getHeight());

        return new Vector2f(instX, instY);
    }

    public static void setCurrentViewportPosition(Vector2f position) {
        get().currentViewportPosition.set(position);
    }

    public static void setCurrentViewportSize(Vector2f size) {
        get().currentViewportSize.set(size);
    }

    public static float getWorldPositionX() {
        if (startupMode) return 0.0f;

        return getWorldPosition().x;
    }

    public static float getWorldPositionY() {
        if (startupMode) return 0.0f;

        return getWorldPosition().y;
    }


    public static Vector2f getWorldPosition() {
        if (startupMode || LogicServer.currentScene() == null) return new Vector2f(0.0f, 0.0f);

        float currentX = getX() - get().currentViewportPosition.x;
        currentX = (2.0f * (currentX / get().currentViewportSize.x)) - 1.0f;
        float currentY = (getY() - get().currentViewportPosition.y);
        currentY = (2.0f * (1.0f - (currentY / get().currentViewportSize.y))) - 1;

        Viewport camera = LogicServer.currentScene().viewport();

        if (camera == null) return new Vector2f(0.0f, 0.0f);

        Vector4f tmp = new Vector4f(currentX, currentY, 0, 1);

        Matrix4f inverseView = new Matrix4f(camera.getInverseViewMatrix());
        Matrix4f inverseProjection = new Matrix4f(camera.getInverseProjectionMatrix());

        tmp.mul(inverseView.mul(inverseProjection));

        get().worldCurrentX = tmp.x;
        get().worldCurrentY = tmp.y;

        return new Vector2f(tmp.x, tmp.y);
    }
    //--------------------------------------------------------------------
    // Screen Coordinate = P * V * M
    // World Coordinate = S * V^-1 * p^-1

    public static Vector2f screen2WorldCoordinate(Vector2f screenCoordinate) {
        if (startupMode || LogicServer.currentScene() == null) return new Vector2f(0.0f, 0.0f);

        Vector2f normalization = new Vector2f(
                screenCoordinate.x / Window.getWidth(),
                screenCoordinate.y / Window.getHeight()
        );

        normalization.mul(2f).sub(new Vector2f(1f, 1f));

        Viewport viewport = LogicServer.currentScene().viewport();

        if (viewport == null) return new Vector2f(0.0f, 0.0f);

        Vector4f tmp = new Vector4f(normalization.x, normalization.y, 0 , 1);

        Matrix4f inverseView = new Matrix4f(viewport.getInverseViewMatrix());
        Matrix4f inverseProjection = new Matrix4f(viewport.getInverseProjectionMatrix());

        tmp.mul(inverseView.mul(inverseProjection));

        return new Vector2f(tmp.x, tmp.y);
    }

    public static Vector2f world2ScreenCoordinate(Vector2f worldCoordinate) {
        if (startupMode || LogicServer.currentScene() == null) return new Vector2f(0.0f, 0.0f);

        Viewport viewport = LogicServer.currentScene().viewport();

        if (viewport == null) return new Vector2f(0.0f, 0.0f);

        Vector4f normalization = new Vector4f(worldCoordinate.x, worldCoordinate.y, 0, 1);

        Matrix4f view = new Matrix4f(viewport.getViewMatrix());
        Matrix4f projection = new Matrix4f(viewport.getProjectionMatrix());

        normalization.mul(projection.mul(view));

        Vector2f windowSpace = new Vector2f(normalization.x, normalization.y).
                mul(1f / normalization.w);

        windowSpace.add(new Vector2f(1f, 1f)).mul(0.5f);
        windowSpace.mul(new Vector2f(Window.getWidth(), Window.getHeight()));

        return windowSpace;
    }

    private static boolean isKeyValid(int keyCode) {
        return keyCode >= 0 && keyCode <= GLFW_MOUSE_BUTTON_LAST;
    }
}

package TCB_Field;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public class KeyListener {
    private static KeyListener instance;
    private final boolean[] keyTapped = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] keyPressed = new boolean[GLFW_KEY_LAST + 1];
    private int mods;

    private KeyListener() {}
    public static KeyListener get() {
        if (KeyListener.instance == null) {
            KeyListener.instance = new KeyListener();
        }
        return KeyListener.instance;
    }

    public static void keyCallback(long window, int key, int scancode, int action, int mods) {
        KeyListener listener = get();
        listener.mods = mods;

        if (action == GLFW_PRESS) {
            listener.keyPressed[key] = true;
            listener.keyTapped[key] = true;
        } else if (action == GLFW_RELEASE) {
            listener.keyPressed[key] = false;
            listener.keyTapped[key] = false;
        }
    }

    /**
     * Check if a key is pressed.
     * The check return true when the key is down, subsequence frames will be false.
     * The value is reset once the key is lifted.
     * @param keyCode GLFW assigned key code.
     * @return true if a key matched {@code keyCode} is pressed for one frame, false if the same key is not lifted for next frames.
     */
    public static boolean isKeyTapped(int keyCode) {
        return get().keyTapped[keyCode];
    }

    /**
     * Check if a key is being pressed.
     * The check return true when key is still down across frames.
     * @param keyCode GLFW assigned key code.
     * @return true if a key matched {@code keyCode} is being held.
     */
    public static boolean isKeyPressed(int keyCode) {
        return get().keyPressed[keyCode];
    }

    /**
     * Check if a key and a modifier key are pressed.
     * The check returns true when both conditions met once.
     * @param keyCode GLFW assigned key code.
     * @param modCode GLFW assigned modifier key code.
     * @return true if the key combo matched.
     */
    public static boolean isKeyTapped(int keyCode, int modCode) {
        KeyListener listener = get();
        return listener.keyTapped[keyCode] && (listener.mods & modCode) == modCode;
    }

    /**
     * Check if a key and a modifier key are being pressed.
     * The check returns true when both conditions met.
     * @param keyCode GLFW assigned key code.
     * @param modCode GLFW assigned modifier key code.
     * @return true if the key combo matched.
     */
    public static boolean isKeyPressed(int keyCode, int modCode) {
        KeyListener listener = get();
        return listener.keyPressed[keyCode] && (listener.mods & modCode) == modCode;
    }

    public static void endFrame() {
        Arrays.fill(get().keyTapped, false);
        get().mods = 0;
    }
}

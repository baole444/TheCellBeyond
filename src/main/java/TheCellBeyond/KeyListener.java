package TheCellBeyond;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public class KeyListener {
    private static KeyListener instance;
    private final boolean[] keyTapped = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] keyPressed = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] keyReleased = new boolean[GLFW_KEY_LAST + 1];
    private int mods;

    private final StringBuilder textInput = new StringBuilder();
    private boolean hasTextInput = false;

    private KeyListener() {}
    public static KeyListener get() {
        if (KeyListener.instance == null) {
            KeyListener.instance = new KeyListener();
        }
        return KeyListener.instance;
    }

    public static synchronized void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (!isKeyValid(key)) return;

        KeyListener listener = get();
        if (listener == null) return;

        listener.mods = mods;

        if (action == GLFW_PRESS) {
            listener.keyPressed[key] = true;
            listener.keyTapped[key] = true;
            listener.keyReleased[key] = false;
            return;
        }

        if (action == GLFW_RELEASE) {
            listener.keyPressed[key] = false;
            listener.keyTapped[key] = false;
            listener.keyReleased[key] = true;
        }
    }

    public static void charCallback(long window, int codepoint) {
        KeyListener listener = get();
        if (listener == null) return;

        char c = (char) codepoint;
        listener.textInput.append(c);
        listener.hasTextInput = true;
    }

    /**
     * Get accumulated text since last frame
     * @return the string of accumulated text
     */
    public static String getTextInput() {
        KeyListener listener = get();
        if (listener == null) return "";

        String result = listener.textInput.toString();
        listener.textInput.setLength(0);
        listener.hasTextInput = false;
        return result;
    }

    public static boolean hasTextInput() {
        KeyListener listener = get();
        if (listener == null) return false;

        return listener.hasTextInput;
    }

    /**
     * Check if a key is pressed in this frame.
     * @param keyCode GLFW assigned key code
     * @return true if the key is pressed in the same frame.
     */
    public static boolean isKeyTapped(int keyCode) {
        KeyListener listener = get();
        if (listener == null || !isKeyValid(keyCode)) return false;

        return listener.keyTapped[keyCode];
    }

    /**
     * Check if a key is being pressed.
     * @param keyCode GLFW assigned key code
     * @return true if the key is being held down.
     */
    public static boolean isKeyPressed(int keyCode) {
        KeyListener listener = get();
        if (listener == null || !isKeyValid(keyCode)) return false;

        return listener.keyPressed[keyCode];
    }

    /**
     * Check if a key is just released in this frame.
     * @param keyCode GLFW assigned key code
     * @return true if the key is released in the same frame.
     */
    public static boolean isKeyReleased(int keyCode) {
        KeyListener listener = get();
        if (listener == null || !isKeyValid(keyCode)) return false;

        return listener.keyReleased[keyCode];
    }

    /**
     * Check if a key and a modifier key are pressed for this frame.
     * @param keyCode GLFW assigned key code
     * @param modCode GLFW assigned modifier key code
     * @return true if the key combo matched in the same frame.
     */
    public static boolean isKeyTapped(int keyCode, int modCode) {
        KeyListener listener = get();
        if (listener == null || !isKeyValid(keyCode)) return false;

        return listener.keyTapped[keyCode] && (listener.mods & modCode) == modCode;
    }

    /**
     * Check if a key and a modifier key are being pressed.
     * @param keyCode GLFW assigned key code
     * @param modCode GLFW assigned modifier key code
     * @return true if the key combo matched.
     */
    public static boolean isKeyPressed(int keyCode, int modCode) {
        KeyListener listener = get();
        if (listener == null || !isKeyValid(keyCode)) return false;

        return listener.keyPressed[keyCode] && (listener.mods & modCode) == modCode;
    }

    /**
     * Check if a key and a modifier key are just released for this frame.
     * @param keyCode GLFW assigned key code
     * @param modCode GLFW assigned modifier key code
     * @return true if the key combo matched in the same frame.
     */
    public static boolean isKeyReleased(int keyCode, int modCode) {
        KeyListener listener = get();
        if (listener == null || !isKeyValid(keyCode)) return false;

        return listener.keyReleased[keyCode] && (listener.mods & modCode) == modCode;
    }

    public static void endFrame() {
        KeyListener listener = get();
        if (listener == null) return;

        Arrays.fill(listener.keyTapped, false);
        Arrays.fill(listener.keyReleased, false);
        listener.mods = 0;
    }

    private static boolean isKeyValid(int keyCode) {
        return keyCode >= 0 && keyCode <= GLFW_KEY_LAST;
    }
}

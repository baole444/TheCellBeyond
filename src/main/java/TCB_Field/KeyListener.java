package TCB_Field;

import java.security.Key;
import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public class KeyListener {
    private static KeyListener instance;
    private final boolean[] keyTapped = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] keyPressed = new boolean[GLFW_KEY_LAST + 1];
    private int mods;

    private StringBuilder textInput = new StringBuilder();
    private boolean hasTextInput = false;

    private KeyListener() {}
    public static KeyListener get() {
        if (KeyListener.instance == null) {
            KeyListener.instance = new KeyListener();
        }
        return KeyListener.instance;
    }

    public static void keyCallback(long window, int key, int scancode, int action, int mods) {
        // don't process for unknown key input
        if (!isKeyValid(key)) return;

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

    // For IME composition
    public static void charCallback(long window, int codepoint) {
        KeyListener listener = get();
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
        String result = listener.textInput.toString();
        listener.textInput.setLength(0);
        listener.hasTextInput = false;
        return result;
    }

    public static boolean hasTextInput() {
        return get().hasTextInput;
    }

    /**
     * Check if a key is pressed.
     * The check return true when the key is down, subsequence frames will be false.
     * The value is reset once the key is lifted.
     * @param keyCode GLFW assigned key code.
     * @return true if a key matched {@code keyCode} is pressed for one frame, false if the same key is not lifted for next frames.
     */
    public static boolean isKeyTapped(int keyCode) {
        if (!isKeyValid(keyCode)) return false;

        return get().keyTapped[keyCode];
    }

    /**
     * Check if a key is being pressed.
     * The check return true when key is still down across frames.
     * @param keyCode GLFW assigned key code.
     * @return true if a key matched {@code keyCode} is being held.
     */
    public static boolean isKeyPressed(int keyCode) {
        if (!isKeyValid(keyCode)) return false;

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

        if (!isKeyValid(keyCode)) return false;

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

        if (!isKeyValid(keyCode)) return false;

        return listener.keyPressed[keyCode] && (listener.mods & modCode) == modCode;
    }

    public static void endFrame() {
        Arrays.fill(get().keyTapped, false);
        get().mods = 0;
    }

    private static boolean isKeyValid(int keyCode) {
        return keyCode >= 0 && keyCode <= GLFW_KEY_LAST;
    }
}

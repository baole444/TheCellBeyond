package TCB_Field;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public class KeyListener {
    private static KeyListener instance;
    private boolean keyTapped[] = new boolean[GLFW_KEY_LAST + 1];
    private boolean keyPressed[] = new boolean[GLFW_KEY_LAST + 1];
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

    public static boolean isKeyTapped(int keyCode) {
        boolean rs = get().keyTapped[keyCode];
        if (rs) {
            get().keyTapped[keyCode] = false;
        }
        return rs;
    }

    public static boolean isKeyPressed(int keyCode) {
        return get().keyPressed[keyCode];
    }

    public static boolean isKeyPressed(int keyCode, int modCode) {
        KeyListener listener = get();
        return listener.keyPressed[keyCode] && (listener.mods & modCode) == modCode;
    }

    public static void endFrame() {
        Arrays.fill(get().keyPressed, false);
    }
}

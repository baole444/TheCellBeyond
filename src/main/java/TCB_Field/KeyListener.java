package TCB_Field;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public class KeyListener {
    private static KeyListener instance;
    private boolean keyTapped[] = new boolean[GLFW_KEY_LAST + 1];
    private boolean keyPressed[] = new boolean[GLFW_KEY_LAST + 1];

    private KeyListener() {}
    public static KeyListener get() {
        if (KeyListener.instance == null) {
            KeyListener.instance = new KeyListener();
        }
        return KeyListener.instance;
    }

    public static void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS) {
            get().keyPressed[key] = true;
            get().keyTapped[key] = true;
        } else if (action == GLFW_RELEASE) {
            get().keyPressed[key] = false;
            get().keyTapped[key] = false;
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

    public static void endFrame() {
        Arrays.fill(get().keyPressed, false);
    }
}

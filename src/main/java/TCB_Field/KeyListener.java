package TCB_Field;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class KeyListener {
    private static KeyListener instance;
    private boolean keyTapped[] = new boolean[350];
    private boolean keyPressed[] = new boolean[350];

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

    public static boolean isKeyPressed(int keyCode) {
        return get().keyPressed[keyCode];
    }

    public static boolean isKeyTapped(int keyCode) {
        boolean rs = get().keyTapped[keyCode];
        if (rs) {
            get().keyTapped[keyCode] = false;
        }
        return rs;
    }
}

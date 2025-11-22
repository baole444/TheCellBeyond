package TheCellBeyond;

import org.lwjgl.glfw.GLFW;
import project.Project;
import utility.log.EngineLog;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;

public class Input {
    private static final HashMap<Integer, String> keyNames = new HashMap<>();

    static {
        boolean success = loadKeyCodeName();
        if (!success) {
            for (int i = GLFW_MOUSE_BUTTON_1; i <= GLFW_MOUSE_BUTTON_LAST; i++) {
                keyNames.put(i, "Mouse " + i);
            }

            for (int i = GLFW_MOUSE_BUTTON_LAST + 1; i <= GLFW_KEY_LAST; i++) {
                keyNames.put(i, "Key" + i);
            }
        }
    }

    public static String getKeyName(int keyCode) {
        if (keyCode < 0) return "Unknow key (Code " + keyCode + ")";

        return keyNames.get(keyCode);
    }

    public static boolean isActionJustPressed(String actionName) {
        InputAction action = getInputAction(actionName);
        if (action == null) return false;

        for (Set<InputKey> combo : action.keys()) {
            if (isKeyComboJustPressed(combo)) return true;
        }

        return false;
    }

    public static boolean isActionPresses(String actionName) {
        InputAction action = getInputAction(actionName);
        if (action == null) return false;

        for (Set<InputKey> combo : action.keys()) {
            if (isKeyComboPressed(combo)) return true;
        }

        return false;
    }

    public static boolean isActionJustReleased(String actionName) {
        InputAction action = getInputAction(actionName);
        if (action == null) return false;

        for (Set<InputKey> combo : action.keys()) {
            if (isKeyComboJustReleased(combo)) return true;
        }

        return false;
    }

    private static boolean isKeyComboPressed(Set<InputKey> keys) {
        boolean pressed;
        for (InputKey input : keys) {
            pressed = switch (input.type()) {
                case Keyboard -> KeyListener.isKeyPressed(input.code());
                case Mouse -> MouseListener.isButtonPressed(input.code());
            };

            if (!pressed) return false;
        }

        return true;
    }

    private static boolean isKeyComboJustPressed(Set<InputKey> keys) {
        boolean tapped = false;

        for (InputKey input : keys) {
            boolean pressed = switch (input.type()) {
                case Keyboard -> KeyListener.isKeyPressed(input.code());
                case Mouse -> MouseListener.isButtonPressed(input.code());
            };

            if (!pressed) return false;

            boolean justPressed = switch (input.type()) {
                case Keyboard ->  KeyListener.isKeyTapped(input.code());
                case Mouse -> MouseListener.isButtonPressed(input.code()) && !MouseListener.isDragging();
            };

            if (justPressed) tapped = true;
        }

        return tapped;
    }

    private static boolean isKeyComboJustReleased(Set<InputKey> keys) {
        boolean released = false;

        for (InputKey input : keys) {
            boolean pressed = switch (input.type()) {
                case Keyboard -> KeyListener.isKeyPressed(input.code());
                case Mouse -> MouseListener.isButtonPressed(input.code());
            };

            if (pressed) return false;

            boolean justReleased = switch (input.type()) {
                case Keyboard -> KeyListener.isKeyReleased(input.code());
                case Mouse -> MouseListener.isButtonReleased(input.code());
            };

            if (justReleased) released = true;
        }

        return released;
    }

    private static InputAction getInputAction(String action) {
        if (Project.currentProject() == null) return null;
        return Project.currentProject().inputActions().get(action);
    }

    private static boolean loadKeyCodeName() {
        boolean success = false;
        try {
            Field[] fields = GLFW.class.getDeclaredFields();
            for (Field field : fields) {
                int mod = field.getModifiers();
                if (field.getType() != int.class || !Modifier.isStatic(mod) || !Modifier.isFinal(mod)) continue;

                String name = field.getName();
                if (!name.startsWith("GLFW_KEY_") && !name.startsWith("GLFW_MOUSE_BUTTON_")) continue;

                int val = field.getInt(null);
                String friendlyName = name.replace("GLFW_KEY_", "")
                        .replace("GLFW_MOUSE_BUTTON_", "Mouse ")
                        .replace("_", " ");

                keyNames.put(val, friendlyName);
                success = true;
            }
        } catch (IllegalAccessException e) {
            EngineLog.error("Input System", "Failed to load key code names: " + e.getMessage());
        }

        return success;
    }
}

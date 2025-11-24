package TheCellBeyond;

import org.lwjgl.glfw.GLFW;
import project.Project;
import utility.log.EngineLog;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.*;

public class Input {
    private static final HashMap<Integer, String> keyNames = new HashMap<>();
    private static final HashSet<Integer> modifierKeysCode = new HashSet<>();

    static {
        loadModifierKeyCodes();

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

    public static HashSet<Integer> getModifierKeysCodes() {
        return new HashSet<>(modifierKeysCode);
    }

    public static boolean isModifierKey(int keyCode) {
        return modifierKeysCode.contains(keyCode);
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

    private static void loadModifierKeyCodes() {
        modifierKeysCode.clear();
        modifierKeysCode.add(GLFW_KEY_LEFT_ALT);
        modifierKeysCode.add(GLFW_KEY_RIGHT_ALT);
        modifierKeysCode.add(GLFW_KEY_LEFT_CONTROL);
        modifierKeysCode.add(GLFW_KEY_RIGHT_CONTROL);
        modifierKeysCode.add(GLFW_KEY_LEFT_SHIFT);
        modifierKeysCode.add(GLFW_KEY_RIGHT_SHIFT);
        modifierKeysCode.add(GLFW_KEY_LEFT_SUPER);
        modifierKeysCode.add(GLFW_KEY_RIGHT_SUPER);
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

                if (friendlyName.length() > 1) friendlyName = toCapitalizeFully(friendlyName.toLowerCase());
                keyNames.put(val, friendlyName);
                success = true;
            }
        } catch (IllegalAccessException e) {
            EngineLog.error("Input System", "Failed to load key code names: " + e.getMessage());
        }

        return success;
    }

    private static String toCapitalizeFully(String input) {
        return Arrays.stream(input.split("\\s+"))
                .map(w -> w.substring(0, 1).toUpperCase() + w.substring(1))
                .collect(Collectors.joining(" "));
    }
}

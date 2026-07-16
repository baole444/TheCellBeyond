package TheCellBeyond;

import org.lwjgl.glfw.GLFW;
import project.Project;
import scripting.API;
import utility.log.EngineLog;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Input is a collection of static methods, used to check for input with {@link InputAction}.
 * <p>
 * It also contains method to retrieve user-friendly name for input code and method to check if a key is modifier key or a mouse button.
 */
@API
public final class Input {
    private static final HashMap<Integer, String> keyNames = new HashMap<>();
    private static final HashSet<Integer> modifierKeysCode = new HashSet<>();
    private Input() {}

    static {
        loadModifierKeyCodes();
        boolean success = loadKeyCodeName();
        if (!success) {
            for (int i = GLFW_MOUSE_BUTTON_1; i <= GLFW_MOUSE_BUTTON_LAST; i++) keyNames.put(i, "Mouse " + i);
            for (int i = GLFW_MOUSE_BUTTON_LAST + 1; i <= GLFW_KEY_LAST; i++) keyNames.put(i, "Key" + i);
        }
    }

    /**
     * Gey the name for an input using the given key code.
     * @param keyCode the key code to check for
     * @return a user-friendly name of that input
     */
    public static String getKeyName(int keyCode) {
        if (keyCode < 0) return "Unknow key (Code " + keyCode + ")";
        return keyNames.get(keyCode);
    }

    /**
     * Get the set of modifier key codes of which this engine recognized.
     * @return of copy of modifier key code set
     */
    public static HashSet<Integer> getModifierKeysCodes() {
        return new HashSet<>(modifierKeysCode);
    }

    /**
     * Check if a keycode belong to a modifier key or not.
     * @param keyCode the key code to check
     * @return true if belong to one of the modifier key
     */
    public static boolean isModifierKey(int keyCode) {
        return modifierKeysCode.contains(keyCode);
    }

    /**
     * Check if a keycode belong to a mouse button or not.
     * @param keyCode the key code to check
     * @return true if belong to one of the mouse button
     */
    public static boolean isMouseButton(int keyCode) {
        return keyCode >= GLFW_MOUSE_BUTTON_1 && keyCode <= GLFW_MOUSE_BUTTON_LAST;
    }

    /**
     * Check if an action is just pressed or not.
     * @param actionName name of the input action to check
     * @return true if the action just pressed
     */
    public static boolean isActionJustPressed(String actionName) {
        InputAction action = getInputAction(actionName);
        return isActionJustPressed(action);
    }

    /**
     * Check if an action is just pressed or not.
     * @param action the action to check
     * @return true if the action just pressed
     */
    public static boolean isActionJustPressed(InputAction action) {
        if (action == null) return false;
        for (Set<InputKey> combo : action.keys()) {
            if (isKeyComboJustPressed(combo)) return true;
        }
        return false;
    }

    /**
     * Check if an action is being pressed or not.
     * @param actionName the name of the action to check
     * @return true if the action is being presses
     */
    public static boolean isActionPresses(String actionName) {
        InputAction action = getInputAction(actionName);
        return isActionPresses(action);
    }

    /**
     * Check if an action is being pressed or not.
     * @param action the action to check
     * @return true if the action is being presses
     */
    public static boolean isActionPresses(InputAction action) {
        if (action == null) return false;
        for (Set<InputKey> combo : action.keys()) {
            if (isKeyComboPressed(combo)) return true;
        }
        return false;
    }

    /**
     * Check if an action is just released or not.
     * @param actionName the name of the action to check
     * @return true if the action is just released
     */
    public static boolean isActionJustReleased(String actionName) {
        InputAction action = getInputAction(actionName);
        if (action == null) return false;
        for (Set<InputKey> combo : action.keys()) {
            if (isKeyComboJustReleased(combo)) return true;
        }
        return false;
    }

    /**
     * Get an input action using their given name.
     * @param action name of the action to get
     * @return the {@link InputAction} of the same name
     */
    public static InputAction getInputAction(String action) {
        if (Project.currentProject() == null) return null;
        return Project.currentProject().inputActions().get(action);
    }

    /**
     * Get the direction for 2 input actions.
     * <p>
     * If the {@code positive} action is pressed, the direction will be {@code 1}, and {@code -1} for {@code negative} action.
     * If no action was pressed, or both were pressed, the direction will be {@code 0}.
     * </p>
     * @param positive the name of the positive action
     * @param negative the name of the negative action
     * @return the direction value, either -1, 0 , or 1
     */
    public static int actionDirection(String positive, String negative) {
        boolean pos = isActionPresses(positive);
        boolean neg = isActionPresses(negative);
        if (pos == neg) return 0;
        return pos ? 1 : -1;
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

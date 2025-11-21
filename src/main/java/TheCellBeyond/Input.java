package TheCellBeyond;

import project.Project;

import java.util.Set;

public class Input {
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
}

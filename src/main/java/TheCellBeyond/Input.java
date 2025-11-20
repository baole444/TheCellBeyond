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

    //TODO: Implement tracking in MouseListener and KeyListener
    public static boolean isActionJustReleased(String actionName) {
        InputAction action = getInputAction(actionName);
        if (action == null) return false;

        return false;
    }

    private static boolean isKeyComboPressed(Set<InputKey> keys) {
        boolean pressed;
        for (InputKey input : keys) {
            pressed = switch (input.type()) {
                case Keyboard -> KeyListener.isKeyPressed(input.code());
                case Mouse -> MouseListener.mouseButtonDown(input.code());
            };

            if (!pressed) return false;
        }

        return true;
    }

    private static boolean isKeyComboJustPressed(Set<InputKey> keys) {
        boolean tapped = false;

        for (InputKey input : keys) {
            boolean justPress = switch (input.type()) {
                case Keyboard ->  KeyListener.isKeyTapped(input.code());
                case Mouse -> MouseListener.mouseButtonDown(input.code()) && !MouseListener.isDragging();
            };

            boolean pressed = switch (input.type()) {
                case Keyboard -> KeyListener.isKeyPressed(input.code());
                case Mouse -> MouseListener.mouseButtonDown(input.code());
            };

            if (!pressed) return false;
            if (justPress) tapped = true;
        }

        return tapped;
    }

    private static InputAction getInputAction(String action) {
        if (Project.currentProject() == null) return null;
        return Project.currentProject().inputActions().get(action);
    }
}

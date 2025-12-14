package components;

import org.joml.Vector2f;

public class ControllerDirection {
    public final Vector2f directionVector = ControllerDirection.zero();

    public static Vector2f zero() {
        return new Vector2f(0.0f, 0.0f);
    }

    public static Vector2f up() {
        return new Vector2f(0.0f, 1.0f);
    }

    public static Vector2f down() {
        return new Vector2f(0.0f, -1.0f);
    }

    public static Vector2f left() {
        return new Vector2f(-1.0f, 0.0f);
    }

    public static Vector2f right() {
        return new Vector2f(1.0f, 0.0f);
    }
}

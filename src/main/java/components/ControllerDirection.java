package components;

import org.joml.Vector2i;

public class ControllerDirection {
    public String name;
    public Vector2i directionVector;

    public static Vector2i zero() {
        return new Vector2i(0, 0);
    }

    public static Vector2i up() {
        return new Vector2i(0, 1);
    }

    public static Vector2i down() {
        return new Vector2i(0, -1);
    }

    public static Vector2i left() {
        return new Vector2i(-1, 0);
    }

    public static Vector2i right() {
        return new Vector2i(1, 0);
    }
}

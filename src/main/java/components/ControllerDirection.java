package components;

import org.joml.Vector2f;

public record ControllerDirection(Vector2f directionVector) {
    public ControllerDirection() {
        this(ControllerDirection.zero());
    }

    public ControllerDirection(Vector2f directionVector) {
        this.directionVector = new Vector2f(directionVector);
    }

    public ControllerDirection(ControllerDirection direction) {
        this(new Vector2f(direction.directionVector));
    }

    public void set(Vector2f direction) {
        if (direction == null) return;
        directionVector.set(direction);
    }

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

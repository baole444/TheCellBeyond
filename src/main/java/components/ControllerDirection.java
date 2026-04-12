package components;

import org.joml.Vector2f;

/**
 * ControllerDirection is a record wrapper for the direction vector used by a controller binding.
 * @param directionVector the direction vector
 */
public record ControllerDirection(Vector2f directionVector) {
    /**
     * Create a new {@link ControllerDirection}.
     * The new direction is of (0,0), aka no direction.
     * @see #set(Vector2f) Update the direction
     */
    public ControllerDirection() {
        this(ControllerDirection.zero());
    }

    /**
     * Create a new {@link ControllerDirection} with the given direction vector.
     * If the given vector is null, no direction will be set.
     * @param directionVector the direction vector to copy from
     * @see #set(Vector2f) Update the direction
     */
    public ControllerDirection(Vector2f directionVector) {
        this.directionVector = directionVector == null ? new Vector2f() : new Vector2f(directionVector);
    }

    /**
     * Create a new {@link ControllerDirection} from the direction of an existing controller direction.
     * @param direction the controller direction to copy from
     * @see #set(Vector2f) update the direction
     */
    public ControllerDirection(ControllerDirection direction) {
        this(new Vector2f(direction.directionVector));
    }

    /**
     * Update the direction vector of this controller direction.
     * @param direction the direction vector to update with
     */
    public void set(Vector2f direction) {
        if (direction == null) return;
        directionVector.set(direction);
    }

    /**
     * Get a new zero direction vector.
     * @return a new {@link Vector2f} with (0,0)
     */
    public static Vector2f zero() {
        return new Vector2f(0.0f, 0.0f);
    }

    /**
     * Get a new up direction vector.
     * @return a new {@link Vector2f} with (0,1)
     */
    public static Vector2f up() {
        return new Vector2f(0.0f, 1.0f);
    }

    /**
     * Get a new down direction vector.
     * @return a new {@link Vector2f} with (0,-1)
     */
    public static Vector2f down() {
        return new Vector2f(0.0f, -1.0f);
    }

    /**
     * Get a new left direction vector.
     * @return a new {@link Vector2f} with (-1,0)
     */
    public static Vector2f left() {
        return new Vector2f(-1.0f, 0.0f);
    }

    /**
     * Get a new right direction vector.
     * @return a new {@link Vector2f} with (1,0)
     */
    public static Vector2f right() {
        return new Vector2f(1.0f, 0.0f);
    }
}

package flat_physic.primitive;

import org.joml.Vector2f;

public class Ray2D {
    private Vector2f origin;
    private Vector2f head;

    public Ray2D(Vector2f origin, Vector2f head) {
        this.origin = origin;
        this.head = head;
        this.head.normalize();
    }

    public Vector2f loadOrigin() {
        return this.origin;
    }

    public Vector2f loadHead() {
        return this.head;
    }
}

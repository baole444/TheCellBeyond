package flat_physic_deprecated.primitive;

import org.joml.Vector2f;

public class ReturnRayCollision {
    private Vector2f hitPoint;
    private Vector2f normal;
    private float t;
    private boolean hit;

    public ReturnRayCollision() {
        this.hitPoint = new Vector2f();
        this.normal = new Vector2f();
        this.t = -1;
        this.hit = false;
    }

    public void init(Vector2f hitPoint, Vector2f normal, float t, boolean hit) {
        this.hitPoint.set(hitPoint);
        this.normal.set(normal);
        this.t = t;
        this.hit = hit;
    }

    public static void reset(ReturnRayCollision result) {
        if (result != null) {
            result.hitPoint.zero();
            result.normal.set(0, 0);
            result.t = -1;
            result.hit = false;
        }
    }
}

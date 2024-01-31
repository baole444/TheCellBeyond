package physic_2d.components.collider;

import org.joml.Vector2f;
import physic_2d.components.Collider;

public class Collider2D extends Collider {
    private Vector2f halfSize = new Vector2f(1);
    private Vector2f origin = new Vector2f();

    public Vector2f loadHalfSize() {
        return halfSize;
    }

    public void setHalfSize(Vector2f halfSize) {
        this.halfSize = halfSize;
    }

    public Vector2f loadOrigin() {
        return this.origin;
    }
}

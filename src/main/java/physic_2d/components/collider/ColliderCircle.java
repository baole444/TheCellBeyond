package physic_2d.components.collider;

import physic_2d.components.Collider;

public class ColliderCircle extends Collider {
    private float radius = 1.0f;

    public float loadRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
}

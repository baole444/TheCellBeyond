package flat_physic_deprecated.primitive;

import flat_physic_deprecated.hardobject.HardObject;
import org.joml.Vector2f;

public class Circle {
    private float radius = 1.0f;
    private Vector2f origin = new Vector2f();
    private HardObject hardObject = new HardObject();

    public void setRadius(float radius) {
        this.radius = radius;
    }
    public float loadRadius() {
        return this.radius;
    }

    public Vector2f loadCentre() {
        return hardObject.loadPos();
    }
}

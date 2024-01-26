package flat_physic.primitive;

import flat_physic.hardobject.HardObject;
import org.joml.Vector2f;

public class Circle {
    private float radius = 1.0f;

    private HardObject hardObject = null;

    public float loadRadius() {
        return this.radius;
    }

    public Vector2f loadCentre() {
        return hardObject.loadPos();
    }
}

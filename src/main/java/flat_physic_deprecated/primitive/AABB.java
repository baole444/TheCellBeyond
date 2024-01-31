package flat_physic_deprecated.primitive;

import flat_physic_deprecated.hardobject.HardObject;
import org.joml.Vector2f;

//Flat boundary
public class AABB {
    private Vector2f size = new Vector2f();
    private Vector2f isHalf;
    private HardObject hardObject = null;

    public AABB() {
        this.isHalf =new Vector2f(size).mul(0.5f);
    }

    public AABB(Vector2f min, Vector2f max) {
        this.size = new Vector2f(max).sub(min);
        this.isHalf =new Vector2f(size).mul(0.5f);
    }

    public Vector2f loadMin() {
        return new Vector2f(this.hardObject.loadPos()).sub(this.isHalf);
    }

    public Vector2f loadMax() {
        return new Vector2f(this.hardObject.loadPos()).add(this.isHalf);
    }
}

package flat_physic.primitive;

import flat_physic.hardobject.HardObject;
import org.joml.Vector2f;

public class Box2D {
    private Vector2f size = new Vector2f();
    private Vector2f isHalf = new Vector2f();
    private HardObject hardObject = null;

    public Box2D() {
        this.isHalf =new Vector2f(size).mul(0.5f);
    }

    public Box2D(Vector2f min, Vector2f max) {
        this.size = new Vector2f(max).sub(min);
        this.isHalf =new Vector2f(size).mul(0.5f);
    }

    public Vector2f loadMin() {
        return new Vector2f(this.hardObject.loadPos()).sub(this.isHalf);
    }

    public Vector2f loadMax() {
        return new Vector2f(this.hardObject.loadPos()).add(this.isHalf);
    }

    public Vector2f[] loadVertices() {
        Vector2f min = loadMin();
        Vector2f max = loadMax();

        Vector2f[] vertices = {
                new Vector2f(min.x, min.y), new Vector2f(min.x, max.y),
                new Vector2f(max.x, min.y), new Vector2f(max.x, max.y)
        };

        if (hardObject.loadRotate() != 0.0f) {
            for (Vector2f v : vertices) {
                // TODO: implement this
                // Rotate point V2f about centre(Vector2f) by rotation((float)degree)
                //TCB_Math.rotate(v, this.hardObject.loadPos(), this.hardObject.loadRotate());
            }
        }

        return vertices;
    }

    public HardObject loadHardObject() {
        return this.hardObject;
    }
}

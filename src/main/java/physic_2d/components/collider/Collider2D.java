package physic_2d.components.collider;

import org.joml.Vector2f;
import physic_2d.components.Collider;
import render.DebugDraw;

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

    @Override
    public void updateEditor(float dt) {
        Vector2f centre = new Vector2f(this.gameObject.transform.position).add(this.offset);
        DebugDraw.addBox2(centre, this.halfSize, this.gameObject.transform.rotate);
    }
}

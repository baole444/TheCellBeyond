package physic_2d.components.collider;

import components.Component;
import org.joml.Vector2f;
import render.DebugDraw;

public class FlatCircleCollider extends Component {
    // Use for adjusting asset origin and hit box origin
    private Vector2f offset = new Vector2f();

    private float radius = 1.0f;

    public float loadRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public Vector2f loadOffset() {
        return this.offset;
    }

    public void setOffset(Vector2f offset) {
        this.offset.set(offset);
    }

    @Override
    public void editorUpdate(float dt) {
        Vector2f centre = new Vector2f(this.gameObject.transform.position).add(this.offset);
        DebugDraw.addCircle(centre, this.radius);
    }
}

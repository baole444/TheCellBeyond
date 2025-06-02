package physic2d.components.collider;

import components.Component;
import org.joml.Vector2f;
import render.DebugDraw;

public class BoxCollider2D extends Component {
    // Standard box collider

    private Vector2f halfSize = new Vector2f(1);
    private Vector2f origin = new Vector2f();

    // Use for adjusting asset origin and hit box origin
    private Vector2f offset = new Vector2f();

    public Vector2f getHalfSize() {
        return halfSize;
    }

    public void setHalfSize(Vector2f halfSize) {
        this.halfSize = halfSize;
    }

    public Vector2f getOrigin() {
        return this.origin;
    }

    public Vector2f getOffset() {
        return this.offset;
    }

    public void setOffset(Vector2f offset) {
        this.offset.set(offset);
    }

    @Override
    public void editorUpdate(float dt) {
        Vector2f centre = new Vector2f(this.gameObject.transform.position).add(this.offset);
        DebugDraw.addBox2(centre, this.halfSize, this.gameObject.transform.rotate);
    }
}

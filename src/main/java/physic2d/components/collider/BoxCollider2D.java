package physic2d.components.collider;

import components.SpatialComponent;
import org.joml.Vector2f;
import render.DebugDraw;

public class BoxCollider2D extends SpatialComponent {
    // Standard box collider
    private Vector2f halfSize = new Vector2f(1);

    public Vector2f getHalfSize() {
        return halfSize;
    }

    public void setHalfSize(Vector2f halfSize) {
        this.halfSize = halfSize;
        setTransformDirty();
    }

    public Vector2f getEffectiveHalfSize() {
        Vector2f scale = getScale();
        return new Vector2f(halfSize).mul(scale);
    }

    @Override
    public void editorUpdate(float dt) {
        Vector2f centre = getWorldPosition();
        float rotation = getRotation();

        DebugDraw.addBox2(centre, getEffectiveHalfSize(), getRotation());
    }
}

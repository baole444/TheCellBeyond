package physic2d.components.collider;

import components.SpatialComponent;
import org.joml.Vector2f;
import render.DebugDraw;

public class CircleCollider2D extends SpatialComponent {
    private float radius = 1.0f;

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
        setTransformDirty();
    }

    public float getEffectiveRadius() {
        Vector2f scale = getScale();
        return radius * ((scale.x + scale.y) / 2.0f);
    }

    @Override
    public void editorUpdate(float dt) {
        DebugDraw.addCircle(getWorldPosition(), getEffectiveRadius());
    }
}

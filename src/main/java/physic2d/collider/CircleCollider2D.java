package physic2d.collider;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.Shape;
import org.joml.Vector2f;
import render.DebugDraw;

public class CircleCollider2D extends CollisionShape2D {
    private float radius = 0.16f;

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
        setFixtureNeedReset();
    }

    public float getEffectiveRadius() {
        Vector2f scale = getPhysicBodyScale().mul(localScale);
        return radius * ((scale.x + scale.y) / 2.0f);
    }

    @Override
    public Shape createCollisionShape() {
        CircleShape shape = new CircleShape();
        float radius = getEffectiveRadius();
        shape.setRadius(radius);
        shape.m_p.set(localPosition.x, localPosition.y);
        return shape;
    }

    @Override
    protected void drawDebugShape() {
        if (gameObject == null) return;
        Vector2f bodyPosition = getPhysicBodyPosition();
        Vector2f position = new Vector2f(bodyPosition).add(localPosition);
        float radius = getEffectiveRadius();
        DebugDraw.addCircle(position, radius);
    }
}

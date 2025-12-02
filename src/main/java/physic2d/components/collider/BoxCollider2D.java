package physic2d.components.collider;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.joml.Vector2f;
import render.DebugDraw;

public class BoxCollider2D extends CollisionShape2D {
    private final Vector2f halfSize = new Vector2f(0.16f);

    public Vector2f getHalfSize() {
        return new Vector2f(halfSize);
    }

    public void setHalfSize(Vector2f halfSize) {
        this.halfSize.set(halfSize);
        setFixtureNeedReset();
    }

    public Vector2f getEffectiveHalfSize() {
        Vector2f scale = new Vector2f(localScale).mul(getPhysicBodyScale());
        return new Vector2f(halfSize).mul(scale);
    }

    @Override
    public Shape createCollisionShape() {
        PolygonShape shape = new PolygonShape();
        Vector2f effectiveHalfSize = new Vector2f(halfSize).mul(localScale);
        shape.setAsBox(effectiveHalfSize.x, effectiveHalfSize.y, new Vec2(localPosition.x, localPosition.y), (float) Math.toRadians(localRotation));
        return shape;
    }

    @Override
    protected void drawDebugShape() {
        if (gameObject == null) return;

        Vector2f bodyPosition = getPhysicBodyPosition();
        Vector2f position = new Vector2f(bodyPosition).add(localPosition);
        float rotation = localRotation + getPhysicBodyRotation();

        DebugDraw.addBox2(position, getEffectiveHalfSize(), rotation);
    }
}

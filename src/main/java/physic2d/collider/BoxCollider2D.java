package physic2d.collider;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.joml.Vector2f;
import render.DebugDraw;

/**
 * BoxCollider2D is a rectangular 2D collision shape.
 */
public class BoxCollider2D extends CollisionShape2D {
    private final Vector2f halfSize = new Vector2f(0.16f);

    /**
     * Create a new {@link BoxCollider2D} component.
     */
    public BoxCollider2D() {
        String name = BoxCollider2D.class.getSimpleName();
        this(name);
    }

    /**
     * Create a new {@link BoxCollider2D} component with the given name.
     * @param name the new name for the component
     */
    public BoxCollider2D(String name) {
        if (invalidName(name)) name = BoxCollider2D.class.getSimpleName();
        super(name);
    }

    /**
     * Get the half size of the retangle collision shape.
     * @return the half size vector in world units
     * @apiNote
     * Directly modify the vector returned by this method will not trigger fixture update.
     */
    public Vector2f halfSize() {
        return halfSize;
    }

    /**
     * Set the half size of the rectangle collision shape.
     * @param halfSize the half size vector in world unitd
     */
    public void halfSize(Vector2f halfSize) {
        float x = Math.max(MinimumShapeDimension, halfSize.x);
        float y = Math.max(MinimumShapeDimension, halfSize.y);
        this.halfSize.set(x, y);
        setFixtureNeedReset();
    }

    /**
     * Get the effective half size, which is final size used for creating the collision shape.
     * This is a combination of the base half size and the component's global scale.
     * @return the effective half size vector in world units
     */
    public Vector2f getEffectiveHalfSize() {
        Vector2f effectiveHalfSize = new Vector2f(halfSize).mul(globalScale());
        effectiveHalfSize.x = Math.max(effectiveHalfSize.x, MinimumShapeDimension);
        effectiveHalfSize.y = Math.max(effectiveHalfSize.y, MinimumShapeDimension);
        return effectiveHalfSize;
    }

    @Override
    public Shape createCollisionShape() {
        PolygonShape shape = new PolygonShape();
        Vector2f effectiveHalfSize = getEffectiveHalfSize();
        Vector2f localPos = position();
        shape.setAsBox(effectiveHalfSize.x, effectiveHalfSize.y, new Vec2(localPos.x, localPos.y), (float) Math.toRadians(rotation()));
        return shape;
    }

    @Override
    protected void drawDebugShape() {
        if (gameObject == null) return;
        DebugDraw.addBox2(globalPosition(), getEffectiveHalfSize().mul(2.0f), globalRotation());
    }
}

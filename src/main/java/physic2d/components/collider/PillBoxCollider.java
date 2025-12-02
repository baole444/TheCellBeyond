package physic2d.components.collider;

import org.jbox2d.collision.shapes.Shape;
import org.joml.Vector2f;

/**
 * A combination of circle colliders forming the cap
 * of the collider body with one or more box colliders
 * in the mid-section.<br>
 * This forms a pill-shaped collider, reduce chance of
 * edge catching between collision bodies of other objects.
 */
public class PillBoxCollider extends CollisionShape2D {
    private final transient CircleCollider2D headCircle = new CircleCollider2D();
    private final transient CircleCollider2D footCircle = new CircleCollider2D();
    private final transient BoxCollider2D bodyBox = new BoxCollider2D();

    private float width = 0.32f;
    private float height = 0.64f;

    @Override
    public void start() {
        super.start();

        headCircle.gameObject = this.gameObject;
        footCircle.gameObject = this.gameObject;
        bodyBox.gameObject = this.gameObject;

        if (physicBody2D != null) {
            headCircle.setPhysicBody2D(physicBody2D);
            footCircle.setPhysicBody2D(physicBody2D);
            bodyBox.setPhysicBody2D(physicBody2D);
        }

        calculateCollider();
    }

    @Override
    public void editorUpdate(float dt) {
        headCircle.editorUpdate(dt);
        footCircle.editorUpdate(dt);
        bodyBox.editorUpdate(dt);

        if (needsFixtureReset) resetFixture();

    }

    public float width() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
        calculateCollider();
        setFixtureNeedReset();
    }

    public float height() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
        calculateCollider();
        setFixtureNeedReset();
    }

    private void calculateCollider() {
        float radius = width / 4.0f;
        float boxH = height - (2.0f * radius);

        headCircle.setRadius(radius);
        footCircle.setRadius(radius);

        headCircle.setLocalPosition(new Vector2f(0.0f, boxH / 4.0f));
        footCircle.setLocalPosition(new Vector2f(0.0f, -boxH / 4.0f));

        bodyBox.setHalfSize(new Vector2f(width / 2.0f, boxH / 2.0f));
        bodyBox.setLocalPosition(new Vector2f());
    }

    public CircleCollider2D headCircle() {
        return headCircle;
    }

    public CircleCollider2D footCircle() {
        return footCircle;
    }

    public BoxCollider2D bodyBox() {
        return bodyBox;
    }

    @Override
    public Shape createCollisionShape() {
        return null;
    }

    @Override
    protected void drawDebugShape() {}
}

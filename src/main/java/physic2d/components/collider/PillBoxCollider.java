package physic2d.components.collider;

import TheCellBeyond.Window;
import components.Component;
import org.joml.Vector2f;
import physic2d.components.PhysicBody2D;

/**
 * A combination of circle colliders forming the cap
 * of the collider body with one or more box colliders
 * in the mid-section.<br>
 * This forms a pill-shaped collider, reduce chance of
 * edge catching between collision bodies of other objects.
 */
public class PillBoxCollider extends Component {
    // The top section
    private transient CircleCollider2D headCircle = new CircleCollider2D();

    // The bottom section
    private transient CircleCollider2D footCircle = new CircleCollider2D();

    // The middle section.
    private transient BoxCollider2D midBox = new BoxCollider2D();

    // Allow changing size of collision body during runtime.
    /**
     * Flag to indicate if Fixture should
     * be reset on the next frame.
     */
    private transient boolean shouldFixtureReset = false;

    // default width and height
    private float width = 0.32f;
    private float height = 0.64f;

    private Vector2f offset = new Vector2f();

    @Override
    public void start() {
        this.headCircle.gameObject = this.gameObject;
        this.footCircle.gameObject = this.gameObject;
        this.midBox.gameObject = this.gameObject;

        calculateCollider();
    }

    @Override
    public void update(float dt) {
        if (shouldFixtureReset) {
            resetFixtures();
        }
    }

    @Override
    public void editorUpdate(float dt) {
        headCircle.editorUpdate(dt);
        footCircle.editorUpdate(dt);
        midBox.editorUpdate(dt);

        if (shouldFixtureReset) {
            resetFixtures();
        }
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
        calculateCollider();
        resetFixtures();
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
        calculateCollider();
        resetFixtures();
    }

    public Vector2f getOffset() {
        return this.offset;
    }

    public void setOffset(Vector2f offset) {
        this.offset.set(offset);
        calculateCollider();
        resetFixtures();
    }

    private void calculateCollider() {
        float radius = width / 4.0f;
        float boxH = height - 2.0f * radius;

        headCircle.setRadius(radius);
        footCircle.setRadius(radius);
        // Move the top circle up 1/4 of box collider height.
        headCircle.setOffset(new Vector2f(offset).
                add(0, boxH / 4.0f));

        // Move the bottom circle down 1/4 of box collider height.
        footCircle.setOffset(new Vector2f(offset).
                sub(0, boxH / 4.0f));

        midBox.setHalfSize(new Vector2f(width / 2.0f, boxH / 2.0f));

        midBox.setOffset(offset);

    }

    public void resetFixtures() {
        if (Window.getPhysic2D().isLock()) {
            shouldFixtureReset = true;
            return;
        }

        shouldFixtureReset = false;

        if (gameObject != null) {
            PhysicBody2D physicBody2D = gameObject.getComponent(PhysicBody2D.class);

            if (physicBody2D != null) {
                Window.getPhysic2D().resetCollider(physicBody2D, this);
            }
        }
    }

    public CircleCollider2D getHeadCircle() {
        return headCircle;
    }

    public CircleCollider2D getFootCircle() {
        return footCircle;
    }

    public BoxCollider2D getMidBox() {
        return midBox;
    }

    public boolean shouldFixtureReset() {
        return shouldFixtureReset;
    }

    public void setShouldFixtureReset(boolean shouldFixtureReset) {
        this.shouldFixtureReset = shouldFixtureReset;
    }
}

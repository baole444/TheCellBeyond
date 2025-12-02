package physic2d.components.collider;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.Window;
import components.Component;
import org.jbox2d.collision.shapes.Shape;
import org.joml.Vector2f;
import physic2d.Physic2D;
import physic2d.components.PhysicBody2D;

public abstract class CollisionShape2D extends Component {
    protected PhysicBody2D physicBody2D = null;
    protected Vector2f localPosition = new Vector2f();
    protected Vector2f localScale = new Vector2f(1.0f);
    protected float localRotation = 0.0f;

    protected transient boolean needsFixtureReset = false;

    @Override
    public void update(float dt) {
        if (physicBody2D == null) {
            additionalUpdateLogic(dt);
            return;
        }

        if (needsFixtureReset) resetFixture();
        additionalUpdateLogic(dt);
    }

    @Override
    public void editorUpdate(float dt) {
        drawDebugShape();

        if (physicBody2D == null) {
            additionalUpdateLogic(dt);
            return;
        }

        if (needsFixtureReset) resetFixture();
        additionalUpdateLogic(dt);
    }

    public boolean hasPhysicBody() {
        return physicBody2D != null;
    }

    public PhysicBody2D getPhysicBody2D() {
        return physicBody2D;
    }

    public void setPhysicBody2D(PhysicBody2D physicBody2D) {
        if (this.physicBody2D == physicBody2D) return;
        this.physicBody2D = physicBody2D;
        setFixtureNeedReset();
    }

    protected void setFixtureNeedReset() {
        needsFixtureReset = true;
    }

    public void resetFixture() {
        Physic2D physic2D = Window.getPhysic2D();
        if (physic2D == null || physic2D.isLock()) {
            needsFixtureReset = true;
            return;
        }
        needsFixtureReset = false;

        if (gameObject == null || physicBody2D == null) return;
        physic2D.resetCollider(physicBody2D, this);
    }

    public Vector2f getLocalPosition() {
        return new Vector2f(localPosition);
    }

    public void setLocalPosition(float x, float y) {
        localPosition.set(x, y);
        setFixtureNeedReset();
    }

    public void setLocalPosition(Vector2f position) {
        localPosition.set(position);
        setFixtureNeedReset();
    }

    public Vector2f getLocalScale() {
        return new Vector2f(localScale);
    }

    public void setLocalScale(float x, float y) {
        localScale.set(x, y);
        setFixtureNeedReset();
    }

    public void setLocalScale(Vector2f scale) {
        localScale.set(scale);
        setFixtureNeedReset();
    }

    public float getLocalRotation() {
        return localRotation;
    }

    public void setLocalRotation(float rotation) {
        localRotation = rotation;
        setFixtureNeedReset();
    }

    protected Vector2f getPhysicBodyPosition() {
        if (physicBody2D != null) return physicBody2D.getPosition();
        if (gameObject != null && gameObject instanceof GameObject2D gameObject2D) return gameObject2D.getPosition();
        return new Vector2f();
    }

    protected float getPhysicBodyRotation() {
        if (physicBody2D != null) return physicBody2D.getRotation();
        if (gameObject != null && gameObject instanceof GameObject2D gameObject2D) return gameObject2D.getRotation();
        return 0.0f;
    }

    protected Vector2f getPhysicBodyScale() {
        if (physicBody2D != null) return physicBody2D.getScale();
        if (gameObject != null && gameObject instanceof GameObject2D gameObject2D) return gameObject2D.getScale();
        return new Vector2f(1.0f);
    }

    public abstract Shape createCollisionShape();

    protected abstract void drawDebugShape();
}

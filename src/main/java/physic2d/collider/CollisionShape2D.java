package physic2d.collider;

import TheCellBeyond.internal.LogicServer;
import components.SpatialComponent;
import org.jbox2d.collision.shapes.Shape;
import physic2d.Physic2D;
import physic2d.PhysicBody2D;

public abstract class CollisionShape2D extends SpatialComponent {
    public static final float MinimumShapeDimension = 0.001f;
    protected transient PhysicBody2D physicBody2D = null;
    protected transient boolean needsFixtureReset = false;

    @Override
    protected void onStarting() {
        if (gameObject instanceof PhysicBody2D body2D) {
            physicBody2D = body2D;
            needsFixtureReset = true;
            resetFixture();
        }
    }

    @Override
    protected void onDestroy() {
        physicBody2D = null;
    }

    @Override
    protected void additionalDirtyFlagLogic() {
        setFixtureNeedReset();
    }

    @Override
    protected void onUpdate(float dt) {
        if (physicBody2D == null) return;
        drawDebugShape();
    }

    @Override
    public void editorUpdate(float dt) {
        if (needsFixtureReset) resetFixture();
        onEditorUpdate(dt);
        drawDebugShape();
    }

    public boolean hasPhysicBody() {
        return physicBody2D != null;
    }

    public PhysicBody2D getPhysicBody2D() {
        return physicBody2D;
    }

    protected void setFixtureNeedReset() {
        needsFixtureReset = true;
    }

    public void resetFixture() {
        Physic2D physic2D = LogicServer.physic2D();
        if (physic2D == null || physic2D.isLock()) {
            needsFixtureReset = true;
            return;
        }
        needsFixtureReset = false;

        if (physicBody2D == null) return;
        physic2D.resetCollider(physicBody2D, this);
    }

    public abstract Shape createCollisionShape();

    protected abstract void drawDebugShape();
}

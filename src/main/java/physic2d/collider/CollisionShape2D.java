package physic2d.collider;

import TheCellBeyond.internal.LogicServer;
import components.SpatialComponent;
import org.jbox2d.collision.shapes.ChainShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import physic2d.Physic2D;
import physic2d.PhysicBody2D;

public abstract class CollisionShape2D extends SpatialComponent {
    public static final float MinimumShapeDimension = 0.001f;
    protected transient PhysicBody2D physicBody2D = null;
    protected transient boolean needsFixtureReset = false;

    @Override
    protected void onStart() {
        init();
    }

    @Override
    protected void onEditorStart() {
        init();
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

    private void init() {
        if (!(gameObject instanceof PhysicBody2D body2D)) return;
        physicBody2D = body2D;
        needsFixtureReset = true;
        resetFixture();
    }

    protected static Shape createShapeFromNodeArray(Vec2[] vertices) {
        if (!counterClockwise(vertices)) reverse(vertices);
        if (convexPolygon(vertices)) return polygonShape(vertices);
        return chainShape(vertices);
    }

    protected static PolygonShape polygonShape(Vec2[] vertices) {
        PolygonShape shape = new PolygonShape();
        shape.set(vertices, vertices.length);
        return shape;
    }

    protected static ChainShape chainShape(Vec2[] vertices) {
        ChainShape shape = new ChainShape();
        shape.createLoop(vertices, vertices.length);
        return shape;
    }

    protected static boolean counterClockwise(Vec2[] vertices) {
        float sum = 0.0f;
        for (int i = 0; i < vertices.length; i++) {
            Vec2 v1 = vertices[i];
            Vec2 v2 = vertices[(i + 1) % vertices.length];
            sum += (v2.x - v1.x) * (v2.y + v1.y);
        }

        return sum < 0.0f;
    }

    protected static void reverse(Vec2[] vertices) {
        int l = 0;
        int r = vertices.length - 1;
        while (l < r) {
            Vec2 tmp = vertices[l];
            vertices[l] = vertices[r];
            vertices[r] = tmp;
            l++;
            r--;
        }
    }

    protected static boolean convexPolygon(Vec2[] vertices) {
        if (vertices.length < 3) return false;
        boolean positive = false;
        boolean negative = false;
        for (int i = 0; i < vertices.length; i++) {
            Vec2 v1 = vertices[i];
            Vec2 v2 = vertices[(i + 1) % vertices.length];
            Vec2 v3 = vertices[(i + 2) % vertices.length];
            float cross = crossProduct(v1, v2, v3);
            if (cross > 0.0f) positive = true;
            if (cross < 0.0f) negative = true;
            if (positive && negative) return false;
        }

        return true;
    }

    protected static float crossProduct(Vec2 a, Vec2 b, Vec2 c) {
        return (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x);
    }
}


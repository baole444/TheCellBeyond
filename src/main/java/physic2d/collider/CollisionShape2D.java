package physic2d.collider;

import TheCellBeyond.internal.LogicServer;
import components.Component2D;
import org.jbox2d.collision.shapes.ChainShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import physic2d.CollisionObject2D;
import physic2d.Physic2D;
import physic2d.PhysicBody2D;

/**
 * CollisionShape2D is an abstract 2D shape, used as base class for all 2D collision shape component types.
 * <p>
 * CollisionShape2D require the object that mount it to be of type {@link CollisionObject2D} or its subclasses
 * to have full physic interaction.
 */
public abstract class CollisionShape2D extends Component2D {
    /**
     * Minimum allow dimension of a shape (in meter metric.)
     */
    public static final float MinimumShapeDimension = 0.001f;

    /**
     * The reference of the collision object that mount this component.
     */
    protected transient CollisionObject2D collisionObject2D = null;

    /**
     * IS the fixture of this collision shape outdated and need to be updated.
     */
    protected transient boolean needsFixtureReset = false;

    /**
     * Create a {@link CollisionShape2D} component.
     */
    public CollisionShape2D() {
        String name = CollisionShape2D.class.getSimpleName();
        this(name);
    }

    /**
     * Create a {@link CollisionShape2D} component using the given name.
     * @param name the new name for the component
     */
    public CollisionShape2D(String name) {
        if (invalidName(name)) name = CollisionShape2D.class.getSimpleName();
        super(name);
    }

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
        collisionObject2D = null;
    }

    @Override
    protected void onTransformDirty() {
        setFixtureNeedReset();
    }

    @Override
    protected void onPhysicUpdate(float dt) {
        if (needsFixtureReset) resetFixture();
    }

    @Override
    public void editorUpdate(float dt) {
        if (needsFixtureReset) resetFixture();
        onEditorUpdate(dt);
        drawDebugShape();
    }

    /**
     * Check if this collision shape has physic body reference.
     * @return true if physic body exist for this component
     */
    public boolean hasCollisionObject() {
        return collisionObject2D != null;
    }

    /**
     * Get the physic body reference of this collision shape.
     * @return the {@link CollisionObject2D} object that mounted this component
     */
    public CollisionObject2D collisionObject2D() {
        return collisionObject2D;
    }

    /**
     * Toggle the flag for fixture reset.
     */
    protected void setFixtureNeedReset() {
        needsFixtureReset = true;
    }

    /**
     * Reset the fixture data.
     * <p>
     * This requires that this component is mounted to a physic object,
     * the physic world of current scene existed and is not locked.
     */
    public void resetFixture() {
        Physic2D physic2D = LogicServer.currentScenePhysic2D();
        if (physic2D == null || physic2D.isLock()) {
            needsFixtureReset = true;
            return;
        }
        needsFixtureReset = false;
        if (collisionObject2D == null) return;
        physic2D.resetCollider(collisionObject2D, this);
    }

    /**
     * Create the collision shape base on the implement requirement.
     * @return the {@link Shape} reference
     */
    public abstract Shape createCollisionShape();

    /**
     * Draw the debug shape of the collision shape.
     */
    protected abstract void drawDebugShape();

    /**
     * Create the polygon/chain collision shape using the given vertex vector array.
     * @param vertices the vertex vector array to create polygon from
     * @return a new {@link Shape}
     */
    protected static Shape createShapeFromNodeArray(Vec2[] vertices) {
        if (!counterClockwise(vertices)) reverse(vertices);
        if (convexPolygon(vertices)) return polygonShape(vertices);
        return chainShape(vertices);
    }

    /**
     * Create the polygon collision shape using the given vertex vector array.
     * <p>
     * This requires that the array does not form a concave polygon and is in counterclockwise order.
     * @param vertices the array to create polygon from
     * @return a new {@link PolygonShape}
     */
    protected static PolygonShape polygonShape(Vec2[] vertices) {
        PolygonShape shape = new PolygonShape();
        shape.set(vertices, vertices.length);
        return shape;
    }

    /**
     * Create the chain collision shape using the given vertex vector array.
     * @param vertices the array to create chain from
     * @return a new {@link ChainShape}
     */
    protected static ChainShape chainShape(Vec2[] vertices) {
        ChainShape shape = new ChainShape();
        shape.createLoop(vertices, vertices.length);
        return shape;
    }

    /**
     * Check if the vertex vector array form a polygon in counterclockwise order or not
     * @param vertices the array to check
     * @return true if the order is counterclockwise
     */
    protected static boolean counterClockwise(Vec2[] vertices) {
        float sum = 0.0f;
        for (int i = 0; i < vertices.length; i++) {
            Vec2 v1 = vertices[i];
            Vec2 v2 = vertices[(i + 1) % vertices.length];
            sum += (v2.x - v1.x) * (v2.y + v1.y);
        }
        return sum < 0.0f;
    }

    /**
     * Reverse the order of the vertex vector array.
     * @param vertices the array to reverse
     */
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

    /**
     * Check if the vertex vector array can form a convex polygon or not
     * @param vertices the array to check
     * @return true if a convex polygon can be formed
     */
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

    /**
     * Perform cross production between 3 vector
     * @param a vector a
     * @param b vector b
     * @param c vector c
     * @return the cross production of 3 vector
     */
    protected static float crossProduct(Vec2 a, Vec2 b, Vec2 c) {
        return (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x);
    }

    private void init() {
        if (!(gameObject instanceof CollisionObject2D go)) return;
        collisionObject2D = go;
        needsFixtureReset = true;
        resetFixture();
    }
}


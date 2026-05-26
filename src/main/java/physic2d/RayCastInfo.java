package physic2d;

import TheCellBeyond.GameObject;
import org.jbox2d.callbacks.RayCastCallback;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Fixture;
import org.joml.Vector2f;

public class RayCastInfo implements RayCastCallback {
    public Fixture fixture;
    public Vector2f contactedPoint;
    public Vector2f normalDirection;
    /**
     * How far the contacted object is
     * on the Ray Cast vector from the origin Object.
     */
    public float rayVectorFraction;
    public boolean hit;
    public GameObject contactedObject;
    private final GameObject originObject;

    public RayCastInfo(GameObject originObject) {
        fixture = null;
        contactedPoint = new Vector2f();
        normalDirection = new Vector2f();
        rayVectorFraction = 0.0f;
        hit = false;
        contactedObject = null;
        this.originObject = originObject;
    }

    @Override
    public float reportFixture(Fixture fixture, Vec2 point, Vec2 normal, float fraction) {
        if (fixture.m_userData == originObject) return 1.0f;
        if (fixture.isSensor()) return 1.0f;
        if (originObject instanceof CollisionObject2D origin && fixture.m_userData instanceof CollisionObject2D target) {
            if (origin.sharePhysicHierarchy(target)) return 1.0f;
            if ((origin.getCollisionMask() & target.getCollisionLayer()) == 0) return -1.0f;
        }
        this.fixture = fixture;
        this.contactedPoint = new Vector2f(point.x, point.y);
        this.normalDirection = new Vector2f(normal.x, normal.y);
        this.rayVectorFraction = fraction;
        this.hit = fraction != 0.0f;
        this.contactedObject = (GameObject) fixture.m_userData;
        return fraction;
    }
}

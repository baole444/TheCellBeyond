package physic2d;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.internal.LogicServer;
import org.jbox2d.callbacks.RayCastCallback;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Fixture;
import org.joml.Vector2f;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RayCast2D is a ray in 2D world space, used to find the first collision object it intersects.
 * <p>
 * A raycast represents a ray from its origin to its {@link #targetPosition} that finds the closet object that it intersect.
 * Interaction is calculated every physic frame.
 * </p>
 * RayCast2D can ignore objects that are added to its exception list
 * or outside its {@link #collisionMask}.
 */
public class RayCast2D extends GameObject2D {
    /**
     * Should the ray's collisions with {@link Area2D} be reported.
     */
    public boolean collideWithAreas = false;
    /**
     * Should the ray's collisions with {@link PhysicBody2D} be reported.
     */
    public boolean collideWithBodies = true;
    /**
     * The ray's collision mask, only objects in at least one collision enabled in the mask will be detected.
     */
    public int collisionMask = PhysicLayer.layerToBit(0);
    /**
     * Should collisions be reported.
     */
    public boolean enabled = true;
    /**
     * Should this raycast not report collision with parent object. This only matter if the parent object is
     * a {@link CollisionObject2D} or its subclasses.
     * <p>
     * If there is a collision object ancestor, regardless of intermediate none physic objects,
     * the ray will also exclude any collision sibling and descendant object of the same collision ancestor.
     */
    public boolean excludeParent = true;
    /**
     * Should the ray detects hit when starting inside shapes. This will make the collision normal to be (0,0).
     * Does not affect concave polygon shapes.
     */
    public boolean hitFromInside = false;
    /**
     * The ray's destination point, relative to this raycast's {@link #position()}, in world unit.
     */
    public final Vector2f targetPosition = new Vector2f(0.0f, 0.32f);
    /**
     * Is there any object intersecting with the ray's vector or not.
     */
    public transient boolean colliding = false;
    /**
     * The collision object that is intersecting with the ray, or null if there is none.
     */
    public transient CollisionObject2D collider = null;
    /**
     * The collision normal of the intersecting object's shape at the collision point,
     * or (0,0) if the ray starts inside the shape and {@link #hitFromInside} is true.
     * @apiNote to ensure the collision normal is up-to-date, check that {@link #colliding} is true before calling this.
     */
    public transient final Vector2f collisionNormal = new Vector2f();
    /**
     * The world space (global) position where the ray intersects with the closest object, or the origin point of this ray
     * if {@link #hitFromInside} is true and the ray starts inside a collision shape.
     * @apiNote to ensure the collision point is up-to-date, check that {@link #colliding} is true before calling this.
     */
    public transient final Vector2f collisionPoint = new Vector2f();
    /**
     * Objects in this set will be excluded from hit report.
     */
    private final Set<GameObject2D> exceptions = ConcurrentHashMap.newKeySet();
    /**
     * The ray current target, recomputed per physic frame
     */
    private transient final Vector2f currentTarget = new Vector2f();
    /**
     * Callback for this ray with the physic world.
     */
    private transient final RayCast2DCallback callback = new RayCast2DCallback(this);

    /**
     * Create a new {@link RayCast2D}.
     */
    public RayCast2D() {
        String name = RayCast2D.class.getSimpleName();
        super(name);
    }

    /**
     * Create a new {@link RayCast2D} with the given name.
     * @param name the new name for the new ray
     */
    public RayCast2D(String name) {
        if (invalidName(name)) name = RayCast2D.class.getSimpleName();
        super(name);
    }

    /**
     * Add a collision exception so the ray does not report collisions with the specified {@code exception}.
     * @param exception the object to exclude from detection reports
     */
    public void addException(CollisionObject2D exception) {
        if (exception == null) return;
        exceptions.add(exception);
    }

    /**
     * Remove a collision exception so the ray can report collisions with the specified {@code exception}.
     * @param exception the exception object to be reported by detection again
     */
    public void removeException(CollisionObject2D exception) {
        if (exception == null) return;
        exceptions.remove(exception);
    }

    /**
     * Remove all collision exceptions from this ray.
     */
    public void clearException() {
        exceptions.clear();
    }

    @Override
    protected void internalPhysicUpdate(float dt) {
        colliding = false;
        collider = null;
        collisionPoint.zero();
        collisionNormal.zero();
        if (!enabled) return;
        Physic2D physic2D = LogicServer.currentScenePhysic2D();
        if (physic2D == null) return;
        Vector2f origin = globalPosition();
        float rotationRadians = (float) Math.toRadians(globalRotation());
        float cos = (float) Math.cos(rotationRadians);
        float sin = (float) Math.sin(rotationRadians);
        float rotationX = targetPosition.x * cos - targetPosition.y * sin;
        float rotationY = targetPosition.x * sin + targetPosition.y * cos;
        currentTarget.set(origin.x + rotationX, origin.y + rotationY);
        callback.reset();
        physic2D.rayCast(callback, origin, targetPosition);
        if (!callback.hit) return;
        colliding = true;
        collider = callback.hitObject;
        collisionPoint.set(callback.hitPoint);
        collisionNormal.set(callback.hitNormal);
    }

    /**
     * Raycast callback for {@link RayCast2D}.
     */
    private static class RayCast2DCallback implements RayCastCallback {
        private final RayCast2D ray;
        boolean hit = false;
        CollisionObject2D hitObject = null;
        final Vector2f hitPoint = new Vector2f();
        final Vector2f hitNormal = new Vector2f();
        private float closesFraction = 1.0f;

        RayCast2DCallback(RayCast2D ray) {
            this.ray = ray;
        }

        void reset() {
            hit = false;
            hitObject = null;
            closesFraction = 1.0f;
        }

        @Override
        public float reportFixture(Fixture fixture, Vec2 point, Vec2 normal, float fraction) {
            Object userData = fixture.m_userData;
            if (!(userData instanceof CollisionObject2D collisionObject)) return -1.0f;
            if (collisionObject instanceof Area2D && !ray.collideWithAreas) return -1.0f;
            if (collisionObject instanceof PhysicBody2D && !ray.collideWithBodies) return -1.0f;
            if ((ray.collisionMask & collisionObject.getCollisionLayer()) == 0) return -1.0f;
            if (ray.exceptions.contains(collisionObject)) return -1.0f;
            if (ray.excludeParent) {
                CollisionObject2D owner = findCollisionOwner(ray);
                if (owner != null && owner.sharePhysicHierarchy(collisionObject)) return -1.0f;
            }
            if (fraction == 0.0f && !ray.hitFromInside) return -1.0f;
            if (fraction > closesFraction) return -1.0f;
            closesFraction = fraction;
            hit = true;
            hitObject = collisionObject;
            hitPoint.set(point.x, point.y);
            if (fraction == 0.0f) hitNormal.zero();
            else hitNormal.set(normal.x, normal.y);
            return fraction;
        }

        private static CollisionObject2D findCollisionOwner(RayCast2D ray) {
            GameObject parent = ray.getParent();
            while (parent != null) {
                if (parent instanceof CollisionObject2D collisionObject2D) return collisionObject2D;
                parent = parent.getParent();
            }
            return null;
        }
    }
}

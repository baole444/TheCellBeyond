package physic2d;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.internal.LogicServer;
import org.jbox2d.callbacks.QueryCallback;
import org.jbox2d.callbacks.RayCastCallback;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.Collision;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.joml.Math;
import org.joml.Vector2f;
import physic2d.collider.*;
import utility.log.EngineLog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <a href="https://box2d.org">Reference Box2D code (C code)</a>
 */
public final class Physic2D {
    private static final EngineLog Logger = new EngineLog(Physic2D.class);
    /**
     * Maximum physic layer.
     */
    public static final int MaxLayer = 16;
    /**
     * Max velocity calculation pass per physic frame.
     */
    public static final int MaxVelocityPass = 5;
    /**
     * Max position calculation pass per physic frame.
     */
    public static final int MaxPositionPass = 3;
    /**
     * Minimum allowed physic frame rate, the constant physic delta is clamped to this and {@link #MaxPhysicFrameRate}.
     */
    public static final int MinPhysicFrameRate = 5;
    /**
     * Maximum allowed physic frame rate, the constant physic delta is clamped to this and {@link #MinPhysicFrameRate}.
     */
    public static final int MaxPhysicFrameRate = 120;

    private static float physicDeltaRate = 1.0f / 60.0f;
    private static final Vec2 gravity = new Vec2(0, -9.80665f);
    private final World world = new World(gravity);
    private float physicDt = 0.0f;

    private final List<Area2D> monitoringAreas = new ArrayList<>();
    private final List<Fixture> targetFixtures = new ArrayList<>();
    private final Set<GameObject2D> currentBodies = new LinkedHashSet<>();
    private final Set<Area2D> currentAreas = new LinkedHashSet<>();
    private final List<GameObject2D> enteringBodies = new ArrayList<>();
    private final List<GameObject2D> exitingBodies = new ArrayList<>();
    private final List<Area2D> enteringAreas = new ArrayList<>();
    private final List<Area2D> exitingAreas = new ArrayList<>();
    private final AABB queryAABB = new AABB();
    private final AreaQuery areaQuery = new AreaQuery();

    private final class AreaQuery implements QueryCallback {
        private Area2D originArea;
        private Body originBody;

        @Override
        public boolean reportFixture(Fixture fixture) {
            if (fixture.m_userData == originArea || !(fixture.m_userData instanceof CollisionObject2D targetBody)) return true;
            if (targetBody instanceof Area2D targetArea && !targetArea.monitorable) return true;
            if (targetBody.getPhysicBodyRef() == originBody || originArea.sharePhysicHierarchy(targetBody)) return true;
            if ((originArea.getCollisionMask() & targetBody.getCollisionLayer()) == 0) return true;
            targetFixtures.add(fixture);
            return true;
        }
    }

    /**
     * Callback invoked before each physic world step using fixed delta.
     * This allows applying forces or velocities before simulation.
     */
    @FunctionalInterface
    public interface PhysicStepCallback {
        /**
         * Execute physic update logic, called once per physic world step.
         * @param fixedDT the fixed physic delta time
         */
        void onPhysicStep(float fixedDT);
    }

    /**
     * Create new 2D physic world instance.
     */
    public Physic2D() {
        world.setContactListener(new Physic2DContactListener());
    }

    /**
     * Get the current constant physic delta rate of the engine. This is the delta per physic world step
     * not to be confused with the accumulated physic delta time.
     * @return the physic delta rate value
     */
    public static float physicDeltaRate() {
        return physicDeltaRate;
    }

    /**
     * Set the physic frame rate for the engine. This is a safe set for the physic delta rate, clamped by
     * {@link #MinPhysicFrameRate} and {@link #MaxPhysicFrameRate}.
     * @param physicFrameRate the desired physic frame rate to run at
     */
    public static void physicDeltaRate(int physicFrameRate) {
        physicFrameRate = Math.clamp(physicFrameRate, MinPhysicFrameRate, MaxPhysicFrameRate);
        physicDeltaRate = 1.0f / physicFrameRate;
    }

    /**
     * Add an object to the physic world, require the object to be of type {@link CollisionObject2D} or its subclasses
     * to be added to the physic world.
     * @param go the object to add to the physic world
     */
    public void add(GameObject go) {
        if (!(go instanceof CollisionObject2D collisionObject)) return;
        List<CollisionShape2D> collisionShapes = collisionObject.getComponents(CollisionShape2D.class);
        if (collisionObject.getPhysicBodyRef() != null) return;
        Vector2f initialPos = collisionObject.globalPosition();
        float initialRot = collisionObject.globalRotation();
        BodyDef bodyDef = new BodyDef();
        bodyDef.angle = Math.toRadians(initialRot);
        bodyDef.position.set(initialPos.x, initialPos.y);
        bodyDef.userData = collisionObject;
        bodyDef.type = collisionObject.bodyType();
        collisionObject.configureBodyDef(bodyDef);
        Body obj = world.createBody(bodyDef);
        collisionObject.setPhysicBodyRef(obj);
        collisionObject.configurePhysicBodyRef();
        for (CollisionShape2D shape : collisionShapes) {
            if (!shape.hasCollisionObject() || shape.collisionObject2D() != collisionObject) continue;
            addCollider2D(collisionObject, shape);
        }
    }

    /**
     * Destroy the physic representation of the object in the physic world.
     * @param go the object to destroy
     */
    public void destroyObject(GameObject go) {
        if (!(go instanceof CollisionObject2D collisionObject)) return;
        if (collisionObject.getPhysicBodyRef() != null) {
            world.destroyBody(collisionObject.getPhysicBodyRef());
            collisionObject.setPhysicBodyRef(null);
        }
    }

    /**
     * Step the physic world using the fixed delta time {@link #physicDeltaRate} with catchup.
     * This support callbacks to invoke before and/or after each physic world step,
     * @param dt variable frame delta time
     * @param preStepCallback invoked before each physic step, can be null
     * @param postStepCallback invoked after each physic step, can be null
     */
    public void update(float dt, PhysicStepCallback preStepCallback, PhysicStepCallback postStepCallback) {
        physicDt += dt;
        while (physicDt >= physicDeltaRate) {
            physicDt -= physicDeltaRate;
            if (preStepCallback != null) preStepCallback.onPhysicStep(physicDeltaRate);
            world.step(physicDeltaRate, MaxVelocityPass, MaxPositionPass);
            if (postStepCallback != null) postStepCallback.onPhysicStep(physicDeltaRate);
            processAreas();
        }
    }

    /**
     * Step the physic world using the fixed delta time {@link #physicDeltaRate} with catchup.
     * This allows stepping physic world without callbacks.
     */
    public void update(float dt) {
        update(dt, null, null);
    }

    /**
     * Get the interpolation alpha between the accumulated physic delta and the physic delta rate.
     * @return the interpolation alpha value.
     */
    public float interpolateAlpha() {
        return physicDeltaRate > 0.0f ? physicDt / physicDeltaRate : 1.0f;
    }

    /**
     * Update the collision layers and mask of the given collision object.
     * @param collisionObject the object to update
     */
    public void updateBodyFilters(CollisionObject2D collisionObject) {
        Body body = collisionObject.getPhysicBodyRef();
        if (body == null) return;
        int collisionLayer = collisionObject.getCollisionLayer();
        int collisionMask = collisionObject.getCollisionMask();
        Fixture fixture = body.getFixtureList();
        while (fixture != null) {
            fixture.m_filter.categoryBits = collisionLayer;
            fixture.m_filter.maskBits = collisionMask;
            fixture = fixture.m_next;
        }
    }

    /**
     * Add a collision shape to the physic world for the given collision object.
     * @param collisionObject the object to add the shape to
     * @param collisionShape2D the shape to add
     */
    public void addCollider2D(CollisionObject2D collisionObject, CollisionShape2D collisionShape2D) {
        try {
            switch (collisionShape2D) {
                case BoxCollider2D boxCollider2D -> addBoxCollider2D(collisionObject, boxCollider2D);
                case CircleCollider2D circleCollider2D -> addCircleCollider2D(collisionObject, circleCollider2D);
                case CapsuleCollider2D capsuleCollider2D -> addCapsuleCollider(collisionObject, capsuleCollider2D);
                case TileCollider2D tileCollider2D -> addTileCollider2D(collisionObject, tileCollider2D);
                default -> {}
            }
        } catch (Exception e) {
            Logger.error(String.format("Failed to add collider %s of %s : %s", collisionShape2D.name(), collisionObject.name(), e.getMessage()));
        }
    }

    /**
     * Reset and update collision shape in the physic world for the given collision object.
     * @param collisionObject the object to update
     * @param collisionShape2D the shape to add
     */
    public void resetCollider(CollisionObject2D collisionObject, CollisionShape2D collisionShape2D) {
        try {
            Body body = collisionObject.getPhysicBodyRef();
            if (body == null) return;
            int size = fixtureListSize(body);
            for (int i = 0; i < size; i++) body.destroyFixture(body.getFixtureList());
            addCollider2D(collisionObject, collisionShape2D);
            body.resetMassData();
        } catch (Exception e) {
            Logger.error(String.format("Failed to reset collider %s of %s : %s", collisionShape2D.name(), collisionObject.name(), e.getMessage()));
        }
    }

    /**
     * Toggle the sensor collision mod for a collision object.
     * @param collisionObject the object to update
     * @param val true to make collision object to sensor mode
     */
    public void setIsSensor(CollisionObject2D collisionObject, boolean val) {
        Body body = collisionObject.getPhysicBodyRef();
        if (body == null) return;
        Fixture fixture = body.getFixtureList();
        while (fixture != null) {
            fixture.m_isSensor = val;
            fixture = fixture.m_next;
        }
    }

    /**
     * Check if the physic world currently locked.
     * <p>
     * This means the world is updating via physic step and its states become immutable.
     * @return true if locked
     */
    public boolean isLock() {
        return world.isLocked();
    }

    /**
     * Get the gravity vector of the physic engine. This is the value applied to every world instances on created.
     * <p>
     * By default, the gravity point downward, toward negative Y at {@code -9.80665 m/s}.
     * @return a copy of the gravity vector
     * @see #worldGravity()
     */
    public static Vector2f gravity() {
        return new Vector2f(gravity.x, gravity.y);
    }

    /**
     * Get a copy of the current physic world's gravity vector. If there is no world active or created,
     * this will return the physic engine default gravity.
     * <p>
     * By default, the gravity point downward, toward negative Y at {@code -9.80665 m/s}.
     * @return a copy of the physic world's gravity vector
     * @see #gravity() Get the physic engine's gravity vector
     */
    public static Vector2f worldGravity() {
        Physic2D physic = LogicServer.currentScenePhysic2D();
        if (physic == null) return new Vector2f(gravity.x, gravity.y);
        Vec2 worldGravity = physic.world.getGravity();
        return new Vector2f(worldGravity.x, worldGravity.y);
    }

    /**
     * Perform a raycast from an origin to the targeted world space point in the physic world.
     * @param originObject the object that will initiate the raycast
     * @param origin the ray origin in world space
     * @param target the ray target in world space
     * @return a new {@link RayCastInfo} contains the result of the raycast
     */
    public RayCastInfo rayCastInfo(GameObject originObject, Vector2f origin, Vector2f target) {
        RayCastInfo callback = new RayCastInfo(originObject);
        world.raycast(callback,
                new Vec2(origin.x, origin.y),
                new Vec2(target.x, target.y));
        return callback;
    }

    /**
     * Ensure that the callback's data is properly reset for clean result.
     * @param callback the callback for the raycast result
     * @param origin the ray origin in world space
     * @param target the ray target in world space
     */
    void rayCast(RayCastCallback callback, Vector2f origin, Vector2f target) {
        world.raycast(callback, new Vec2(origin.x, origin.y), new Vec2(target.x, target.y));
    }

    /**
     * Perform an AABB overlap query in the physic world, returning all fixtures that overlaps the given region.
     * Only fixtures with collision layer matching the origin object's collision mask are included.
     * @param originObject the object to perform the query with, excluded from result
     * @param aabb the axis aligned bounding box to test against
     * @return a list of overlapping fixtures, filtered by collision mask
     */
    public List<Fixture> queryOverlap(CollisionObject2D originObject, AABB aabb) {
        if (originObject == null || aabb == null) return List.of();
        List<Fixture> result = new ArrayList<>();
        world.queryAABB(fixture -> {
            if (fixture.m_userData == originObject) return true;
            if (fixture.isSensor()) return true;
            if (!(fixture.m_userData instanceof CollisionObject2D target)) return true;
            if (originObject.sharePhysicHierarchy(target)) return true;
            if ((originObject.getCollisionMask() & target.getCollisionLayer()) == 0) return true;
            result.add(fixture);
            return true;
        }, aabb);
        return result;
    }

    private void createFixture(CollisionObject2D collisionObject, Body body, Shape shape) {
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1.0f;
        fixtureDef.friction = collisionObject.friction();
        fixtureDef.restitution = 0.0f;
        fixtureDef.userData = collisionObject;
        fixtureDef.isSensor = collisionObject.isSensor();
        fixtureDef.filter.categoryBits = collisionObject.getCollisionLayer();
        fixtureDef.filter.maskBits = collisionObject.getCollisionMask();
        fixtureDef.filter.groupIndex = 0;
        body.createFixture(fixtureDef);
    }

    private void addBoxCollider2D(CollisionObject2D collisionObject, BoxCollider2D boxCollider2D) {
        Body body = collisionObject.getPhysicBodyRef();
        if (body == null) return;
        Shape shape = boxCollider2D.createCollisionShape();
        createFixture(collisionObject, body, shape);
    }

    private void addCircleCollider2D(CollisionObject2D collisionObject, CircleCollider2D circleCollider2D) {
        Body body = collisionObject.getPhysicBodyRef();
        if (body == null) return;
        Shape shape = circleCollider2D.createCollisionShape();
        createFixture(collisionObject, body, shape);
    }

    private void addCapsuleCollider(CollisionObject2D collisionObject, CapsuleCollider2D capsuleCollider2D) {
        Body body = collisionObject.getPhysicBodyRef();
        if (body == null) return;
        addBoxCollider2D(collisionObject, capsuleCollider2D.bodyBox());
        addCircleCollider2D(collisionObject, capsuleCollider2D.headCircle());
        addCircleCollider2D(collisionObject, capsuleCollider2D.footCircle());
    }

    private void addTileCollider2D(CollisionObject2D collisionObject, TileCollider2D tileCollider2D) {
        Body body = collisionObject.getPhysicBodyRef();
        if (body == null) return;
        Set<Shape> shapes = tileCollider2D.createCollisionShapes();
        for (Shape shape : shapes) {
            if (shape != null) createFixture(collisionObject, body, shape);
        }
    }

    private int fixtureListSize(Body body) {
        if (body == null) return 0;
        int size = 0;
        Fixture fixture = body.getFixtureList();
        while (fixture != null) {
            size++;
            fixture = fixture.m_next;
        }
        return size;
    }

    private void processAreas() {
        if (world.isLocked()) return;
        monitoringAreas.clear();
        for (Body body = world.getBodyList(); body != null; body = body.getNext()) {
            if (!(body.m_userData instanceof Area2D area2D)) continue;
            if (!area2D.monitoring || body.getFixtureList() == null) continue;
            monitoringAreas.add(area2D);
        }
        if (monitoringAreas.isEmpty()) return;
        Collision collision = world.getPool().getCollision();
        for (Area2D area2D : monitoringAreas) processAreaDetection(area2D, collision);
    }

    private void processAreaDetection(Area2D area, Collision collision) {
        Body body = area.getPhysicBodyRef();
        if (body == null || body.getFixtureList() == null) return;
        buildAreaQueryAABB(body);
        targetFixtures.clear();
        areaQuery.originArea = area;
        areaQuery.originBody = body;
        world.queryAABB(areaQuery, queryAABB);
        currentBodies.clear();
        currentAreas.clear();
        for (Fixture fixture : targetFixtures) {
            if (!(fixture.m_userData instanceof CollisionObject2D target)) continue;
            if (alreadyDetected(target)) continue;
            Body targetBody = target.getPhysicBodyRef();
            if (targetBody == null) continue;
            if (!shapesOverlapping(collision, body, fixture, targetBody)) continue;
            if (target instanceof Area2D targetArea) currentAreas.add(targetArea);
            else currentBodies.add(target);
        }
        emitAreaDetection(area);
    }

    private void buildAreaQueryAABB(Body body) {
        boolean initialized = false;
        Fixture fixture = body.getFixtureList();
        while (fixture != null) {
            int childCount = fixture.getShape().getChildCount();
            for (int i = 0; i < childCount; i++) {
                AABB childAABB = fixture.getAABB(i);
                if (initialized) {
                    queryAABB.combine(childAABB);
                    continue;
                }
                queryAABB.set(childAABB);
                initialized = true;
            }
            fixture = fixture.getNext();
        }
    }

    private void emitAreaDetection(Area2D area) {
        enteringBodies.clear();
        exitingBodies.clear();
        enteringAreas.clear();
        exitingAreas.clear();
        for (GameObject2D body : currentBodies) {
            if (!area.trackingBody(body)) enteringBodies.add(body);
        }
        for (GameObject2D body : area.trackedBodies()) {
            if (!currentBodies.contains(body)) exitingBodies.add(body);
        }
        for (Area2D other : currentAreas) {
            if (!area.trackingArea(other)) enteringAreas.add(other);
        }
        for (Area2D other : area.trackedAreas()) {
            if (!currentAreas.contains(other)) exitingAreas.add(other);
        }
        for (GameObject2D body : enteringBodies) {
            area.trackBodyEnter(body);
            area.bodyEntered.emit(body);
        }
        for (Area2D other : enteringAreas) {
            area.trackAreaEnter(other);
            area.areaEntered.emit(other);
        }
        for (GameObject2D body : exitingBodies) {
            area.trackBodyExit(body);
            area.bodyExited.emit(body);
        }
        for (Area2D other : exitingAreas) {
            area.trackAreaExit(other);
            area.areaExited.emit(other);
        }
    }

    private boolean alreadyDetected(CollisionObject2D target) {
        if (target instanceof Area2D targetArea) return currentAreas.contains(targetArea);
        return currentBodies.contains(target);
    }

    private boolean shapesOverlapping(Collision collision, Body areaBody, Fixture targetFixture, Body targetBody) {
        Transform areaTransform = areaBody.getTransform();
        Transform targetTransform = targetBody.getTransform();
        Shape targetShape = targetFixture.getShape();
        int targetChildCount = targetShape.getChildCount();
        Fixture areaFixture = areaBody.getFixtureList();
        while (areaFixture != null) {
            Shape areaShape = areaFixture.getShape();
            int areaChildCount = areaShape.getChildCount();
            for (int areaIndex = 0; areaIndex < areaChildCount; areaIndex++) {
                for (int targetIndex = 0; targetIndex < targetChildCount; targetIndex++) {
                    if (collision.testOverlap(areaShape, areaIndex, targetShape, targetIndex, areaTransform, targetTransform)) return true;
                }
            }
            areaFixture = areaFixture.getNext();
        }
        return false;
    }
}

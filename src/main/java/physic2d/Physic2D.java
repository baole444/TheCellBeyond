package physic2d;

import TheCellBeyond.GameObject;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.joml.Math;
import org.joml.Vector2f;
import physic2d.collider.*;
import utility.log.EngineLog;

import java.util.List;
import java.util.Set;

/**
 * <a href="https://box2d.org">Reference Box2D code (C code)</a>
 */
public class Physic2D {
    public static final EngineLog LOGGER = new EngineLog(Physic2D.class);

    public static final int MaxLayer = 16;
    public static final float PhysicDeltaRate = 1.0f / 60.0f;
    public static final int MaxVelocityPass = 5;
    public static final int MaxPositionPass = 3;

    private final Vec2 gravity = new Vec2(0, -9.80665f);
    private final World world = new World(gravity);

    private transient float physicDt = 0.0f;

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

    public void destroyObject(GameObject go) {
        if (!(go instanceof CollisionObject2D collisionObject)) return;
        if (collisionObject.getPhysicBodyRef() != null) {
            world.destroyBody(collisionObject.getPhysicBodyRef());
            collisionObject.setPhysicBodyRef(null);
        }
    }

    /**
     * Step the physic world using the fixed delta time {@link #PhysicDeltaRate} with catchup.
     * The callback is invoked before each physic step.
     * @param dt variable frame delta time
     * @param callback callback logic, can be null
     */
    public void update(float dt, PhysicStepCallback callback) {
        physicDt += dt;
        while (physicDt >= PhysicDeltaRate) {
            physicDt -= PhysicDeltaRate;
            if (callback != null) callback.onPhysicStep(PhysicDeltaRate);
            world.step(PhysicDeltaRate, MaxVelocityPass, MaxPositionPass);
        }
    }

    /**
     * Step the physic world using the fixed delta time {@link #PhysicDeltaRate} with catchup.
     * This allows stepping physic world without callback.
     */
    public void update(float dt) {
        update(dt, null);
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
            LOGGER.error(String.format("Failed to add collider %s of %s : %s", collisionShape2D.name(), collisionObject.name(), e.getMessage()));
        }

    }

    public void resetCollider(CollisionObject2D collisionObject, CollisionShape2D collisionShape2D) {
        try {
            Body body = collisionObject.getPhysicBodyRef();
            if (body == null) return;
            int size = fixtureListSize(body);
            for (int i = 0; i < size; i++) body.destroyFixture(body.getFixtureList());
            addCollider2D(collisionObject, collisionShape2D);
            body.resetMassData();
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to reset collider %s of %s : %s", collisionShape2D.name(), collisionObject.name(), e.getMessage()));
        }
    }

    public RayCastInfo rayCastInfo(GameObject originObject, Vector2f origin, Vector2f target) {
        RayCastInfo callback = new RayCastInfo(originObject);
        world.raycast(callback,
                new Vec2(origin.x, origin.y),
                new Vec2(target.x, target.y));
        return callback;
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

    public void setIsSensor(CollisionObject2D collisionObject, boolean val) {
        Body body = collisionObject.getPhysicBodyRef();
        if (body == null) return;
        Fixture fixture = body.getFixtureList();
        while (fixture != null) {
            fixture.m_isSensor = val;
            fixture = fixture.m_next;
        }
    }

    public boolean isLock() {
        return world.isLocked();
    }

    public Vector2f getGravity() {
        Vec2 gravity = world.getGravity();
        return new Vector2f(gravity.x, gravity.y);
    }
}

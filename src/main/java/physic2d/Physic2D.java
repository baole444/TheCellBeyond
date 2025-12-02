package physic2d;

import TheCellBeyond.GameObject;
import components.Component;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.joml.Math;
import org.joml.Vector2f;
import physic2d.components.PhysicBody2D;
import physic2d.components.RigidBody2D;
import physic2d.components.collider.BoxCollider2D;
import physic2d.components.collider.CircleCollider2D;
import physic2d.components.collider.CollisionShape2D;
import physic2d.components.collider.PillBoxCollider;

import java.util.ArrayList;
import java.util.List;

/**
 * <a href="https://box2d.org">Reference Box2D code (C code)</a>
 */
public class Physic2D {
    public static final int MaxLayer = 16;
    public static final float PhysicDeltaRate = 1.0f / 60.0f;
    public static final int MaxVelocityPass = 5;
    public static final int MaxPositionPass = 3;

    private final Vec2 gravity = new Vec2(0, -9.80665f);
    private final World world = new World(gravity);

    private transient float physicDt = 0.0f;

    public Physic2D() {
        world.setContactListener(new Physic2DContactListener());
    }

    public void add(GameObject go) {
        List<PhysicBody2D> physicBodies = go.getComponents(PhysicBody2D.class);
        List<CollisionShape2D> collisionShapes = go.getComponents(CollisionShape2D.class);

        for (PhysicBody2D physicBody2D : physicBodies) {
            if (physicBody2D == null || physicBody2D.getPhysicBodyRef() != null) continue;

            Vector2f initialPos = physicBody2D.getPosition();
            float initialRot = physicBody2D.getRotation();

            BodyDef bodyDef = new BodyDef();
            bodyDef.angle = Math.toRadians(initialRot);
            bodyDef.position.set(initialPos.x, initialPos.y);
            bodyDef.userData = physicBody2D.gameObject;

            bodyDef.type = switch (physicBody2D.getPhysicBodyType()) {
                case Kinematic -> BodyType.KINEMATIC;
                case Static -> BodyType.STATIC;
                case Dynamic -> BodyType.DYNAMIC;
            };

            physicBody2D.configureBodyDef(bodyDef);
            Body obj = world.createBody(bodyDef);

            if (physicBody2D instanceof RigidBody2D rigidBody2D) {
                obj.m_mass = rigidBody2D.getMass();
            }

            physicBody2D.setPhysicBodyRef(obj);
            for (CollisionShape2D shape : collisionShapes) {
                if (!shape.hasPhysicBody() || shape.getPhysicBody2D() != physicBody2D) continue;
                switch (shape) {
                    case BoxCollider2D boxCollider2D -> addBoxCollider2D(physicBody2D, boxCollider2D);
                    case CircleCollider2D circleCollider2D -> addCircleCollider2D(physicBody2D, circleCollider2D);
                    case PillBoxCollider pillBoxCollider -> addPillBoxCollider(physicBody2D, pillBoxCollider);
                    default -> {}
                }
            }
        }
    }

    public void destroyObject(GameObject go) {
        PhysicBody2D physicBody2D = go.getFirstComponent(PhysicBody2D.class);
        if (physicBody2D != null) {
            if (physicBody2D.getPhysicBodyRef() != null) {
                world.destroyBody(physicBody2D.getPhysicBodyRef());
                physicBody2D.setPhysicBodyRef(null);
            }
        }
    }

    public void update(float dt) {
        physicDt += dt;
        if (physicDt >= 0.0f) {
            physicDt -= PhysicDeltaRate;
            world.step(PhysicDeltaRate, MaxVelocityPass, MaxPositionPass);
        }
    }

    private void createFixture(PhysicBody2D physicBody2D, Body body, Shape shape) {
        FixtureDef fixtureDef = new FixtureDef();

        fixtureDef.shape = shape;
        fixtureDef.density = 1.0f;
        fixtureDef.friction = physicBody2D.getFriction();
        fixtureDef.userData = physicBody2D.gameObject;
        fixtureDef.isSensor = physicBody2D.isSensor();
        fixtureDef.filter.categoryBits = physicBody2D.getCollisionLayer();
        fixtureDef.filter.maskBits = physicBody2D.getCollisionMask();
        fixtureDef.filter.groupIndex = 0;

        body.createFixture(fixtureDef);
    }

    public void updateBodyFilters(PhysicBody2D physicBody2D) {
        Body body = physicBody2D.getPhysicBodyRef();
        if (body == null) return;

        int collisionLayer = physicBody2D.getCollisionLayer();
        int collisionMask = physicBody2D.getCollisionMask();

        Fixture fixture = body.getFixtureList();
        while (fixture != null) {
            fixture.m_filter.categoryBits = collisionLayer;
            fixture.m_filter.maskBits = collisionMask;
            fixture = fixture.m_next;
        }
    }

    public void addBoxCollider2D(PhysicBody2D physicBody2D, BoxCollider2D boxCollider2D) {
        Body body = physicBody2D.getPhysicBodyRef();
        if (body == null) return;

        Shape shape = boxCollider2D.createCollisionShape();
        createFixture(physicBody2D, body, shape);
    }

    public void addCircleCollider2D(PhysicBody2D physicBody2D, CircleCollider2D circleCollider2D) {
        Body body = physicBody2D.getPhysicBodyRef();
        if (body == null) return;
        Shape shape = circleCollider2D.createCollisionShape();
        createFixture(physicBody2D, body, shape);
    }

    public void addPillBoxCollider(PhysicBody2D physicBody2D, PillBoxCollider pillBoxCollider) {
        Body body = physicBody2D.getPhysicBodyRef();
        if (body == null) return;

        addBoxCollider2D(physicBody2D, pillBoxCollider.bodyBox());
        addCircleCollider2D(physicBody2D, pillBoxCollider.headCircle());
        addCircleCollider2D(physicBody2D, pillBoxCollider.footCircle());
    }

    public void resetCollider(PhysicBody2D physicBody2D, CollisionShape2D collisionShape2D) {
        Body body = physicBody2D.getPhysicBodyRef();
        if (body == null) return;

        int size = fixtureListSize(body);
        for (int i = 0; i < size; i++) {
            body.destroyFixture(body.getFixtureList());
        }

        switch (collisionShape2D) {
            case BoxCollider2D boxCollider2D -> addBoxCollider2D(physicBody2D, boxCollider2D);
            case CircleCollider2D circleCollider2D -> addCircleCollider2D(physicBody2D, circleCollider2D);
            case PillBoxCollider pillBoxCollider -> addPillBoxCollider(physicBody2D, pillBoxCollider);
            default -> {}
        }

        body.resetMassData();
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

    public void setIsSensor(PhysicBody2D physicBody2D, boolean val) {
        Body body = physicBody2D.getPhysicBodyRef();
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

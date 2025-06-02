package physic2d;

import TheCellBeyond.GameObject;
import TheCellBeyond.Transform;
import components.Component;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.joml.Math;
import org.joml.Vector2f;
import physic2d.components.PhysicBody2D;
import physic2d.components.collider.BoxCollider2D;
import physic2d.components.collider.CircleCollider2D;
import physic2d.components.collider.PillBoxCollider;

public class Physic2D {
    // https://box2d.org Check out this website for reference codes

    private final Vec2 gravity = new Vec2(0, -9.80665f);
    private World world = new World(gravity);

    private float physicDt = 0.0f;
    private float physicDtRate = 1.0f / 60.0f; // delta of 60 fps
    private int velocityPassCount = 9;
    private int positionPassCount = 3;

    public Physic2D() {
        world.setContactListener(new Physic2DContactListener());
    }

    public void add(GameObject go) {
        PhysicBody2D physicBody2D = go.getComponent(PhysicBody2D.class);

        // Duplicate prevention
        if (physicBody2D != null && physicBody2D.getInstObjectBody() == null) {
            Transform transform = go.transform;

            // Define rigid body
            BodyDef objDef = new BodyDef();
            objDef.angle = Math.toRadians(transform.rotate);
            objDef.position.set(transform.position.x, transform.position.y);

            objDef.angularDamping = physicBody2D.getRollResistance();
            objDef.linearDamping = physicBody2D.getTranslateResistance();

            objDef.fixedRotation = physicBody2D.isRotatable();

            objDef.userData = physicBody2D.gameObject;

            objDef.bullet = physicBody2D.isNoneStopCollision();

            objDef.gravityScale = physicBody2D.getGravityScale();

            objDef.angularVelocity = physicBody2D.getAngularVelocity();

            switch (physicBody2D.getPhysicBodyType()) {
                case Kinematic: objDef.type = BodyType.KINEMATIC;
                break;
                case Static: objDef.type = BodyType.STATIC;
                break;
                case Dynamic: objDef.type = BodyType.DYNAMIC;
                break;
            }
            Body obj = this.world.createBody(objDef);

            obj.m_mass = physicBody2D.getMass();

            physicBody2D.setInstObjectBody(obj);

            CircleCollider2D circleCollider2D;
            BoxCollider2D boxCollider2D;
            PillBoxCollider pillBoxCollider;

            if ((circleCollider2D =
                    go.getComponent(CircleCollider2D.class)) != null ) {

                addCircleCollider2D(physicBody2D, circleCollider2D);
            }

            if ((boxCollider2D =
                    go.getComponent(BoxCollider2D.class)) != null) {

                addBoxCollider2D(physicBody2D, boxCollider2D);
            }

            if ((pillBoxCollider =
                    go.getComponent(PillBoxCollider.class)) != null) {
                addPillBoxCollider(physicBody2D, pillBoxCollider);
            }
        }
    }

    public void destroyObject(GameObject go) {
        PhysicBody2D physicBody2D = go.getComponent(PhysicBody2D.class);
        if (physicBody2D != null) {
            if (physicBody2D.getInstObjectBody() != null) {
                world.destroyBody(physicBody2D.getInstObjectBody());
                physicBody2D.setInstObjectBody(null);
            }
        }
    }

    public void update(float dt) {
        physicDt += dt;
        // Update only once per 60 frames (Or 1 update / sec)
        // Help fix frame time variable (Might have minor frame skip)
        if (physicDt >= 0.0f) {
            physicDt -= physicDtRate;
            world.step(physicDtRate, velocityPassCount, positionPassCount);
        }
    }

    private void createFixture(PhysicBody2D physicBody2D, Body body, Shape shape) {
        FixtureDef fixtureDef = new FixtureDef();

        fixtureDef.shape = shape;
        fixtureDef.density = 1.0f;

        fixtureDef.friction = physicBody2D.getFriction();

        fixtureDef.userData = physicBody2D.gameObject;

        fixtureDef.isSensor = physicBody2D.isSensor();

        body.createFixture(fixtureDef);
    }

    public void addBoxCollider2D(PhysicBody2D physicBody2D, Component colliderComponent) {
        BoxCollider2D boxCollider2D = (BoxCollider2D) colliderComponent;
        Body body = physicBody2D.getInstObjectBody();
        assert body != null : "Instant physical body of Object not found.";

        PolygonShape shape = new PolygonShape();

        Vector2f halfSize = new Vector2f(boxCollider2D.getHalfSize()).mul(0.5f); // Applying correct collider box size

        Vector2f offset = boxCollider2D.getOffset();

        shape.setAsBox(halfSize.x, halfSize.y, new Vec2(offset.x, offset.y), 0);

        createFixture(physicBody2D, body, shape);
    }

    public void addCircleCollider2D(PhysicBody2D physicBody2D, Component colliderComponent) {
        CircleCollider2D circleCollider2D = (CircleCollider2D) colliderComponent;
        Body body = physicBody2D.getInstObjectBody();
        assert body != null : "Instant physical body of Object not found.";

        CircleShape shape = new CircleShape();

        shape.setRadius(circleCollider2D.getRadius());

        Vec2 offset = new Vec2(circleCollider2D.getOffset().x, circleCollider2D.getOffset().y);

        shape.m_p.set(offset);

        createFixture(physicBody2D, body, shape);
    }

    public void addPillBoxCollider(PhysicBody2D physicBody2D, Component colliderComponent) {
        PillBoxCollider pillBoxCollider = (PillBoxCollider) colliderComponent;
        Body body = physicBody2D.getInstObjectBody();
        assert body != null : "Instant physical body of Object not found.";

        addBoxCollider2D(physicBody2D, pillBoxCollider.getMidBox());
        addCircleCollider2D(physicBody2D, pillBoxCollider.getHeadCircle());
        addCircleCollider2D(physicBody2D, pillBoxCollider.getFootCircle());
    }

    public void resetCollider(PhysicBody2D physicBody2D, Component colliderObject) {
        Body body = physicBody2D.getInstObjectBody();

        if (body == null) return;

        int size = fixtureListSize(body);

        for (int i = 0; i < size; i++) {
            body.destroyFixture(body.getFixtureList());
        }

        if (colliderObject instanceof BoxCollider2D) {
            addBoxCollider2D(physicBody2D, colliderObject);
        }
        else if (colliderObject instanceof CircleCollider2D) {
            addCircleCollider2D(physicBody2D, colliderObject);
        }
        else if (colliderObject instanceof PillBoxCollider) {
            addPillBoxCollider(physicBody2D, colliderObject);
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
        int size = 0;

        Fixture fixture = body.getFixtureList();
        while (fixture != null) {
            size++;
            fixture = fixture.m_next;
        }

        return size;
    }

    public void setIsSensor(PhysicBody2D physicBody2D, boolean val) {
        Body body = physicBody2D.getInstObjectBody();
        if (body != null) return;

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
        Vec2 gravity = this.world.getGravity();
        return new Vector2f(gravity.x, gravity.y);
    }
}

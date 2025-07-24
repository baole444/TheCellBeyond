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
import physic2d.components.collider.BoxCollider2D;
import physic2d.components.collider.CircleCollider2D;
import physic2d.components.collider.PillBoxCollider;

public class Physic2D {
    // https://box2d.org Check out this website for reference codes

    private final Vec2 gravity = new Vec2(0, -9.80665f);
    private World world = new World(gravity);

    private float physicDt = 0.0f;
    private final float physicDtRate = 1.0f / 60.0f; // delta of 60 fps
    private int velocityPassCount = 9;
    private int positionPassCount = 3;

    public Physic2D() {
        world.setContactListener(new Physic2DContactListener());
    }

    // TODO: For now, limit to 1 collider and 1 physic body, will extend to multi collider when needed
    public void add(GameObject go) {
        PhysicBody2D physicBody2D = go.getFirstComponent(PhysicBody2D.class);

        // Duplicate prevention
        if (physicBody2D != null && physicBody2D.getPhysicBodyRef() == null) {
            Vector2f initialPos = physicBody2D.getWorldPosition();
            float initialRot = physicBody2D.getRotation();

            // Define rigid body
            BodyDef bodyDef = new BodyDef();
            bodyDef.angle = Math.toRadians(initialRot);
            bodyDef.position.set(initialPos.x, initialPos.y);

            bodyDef.angularDamping = physicBody2D.getRollResistance();
            bodyDef.linearDamping = physicBody2D.getTranslateResistance();

            bodyDef.fixedRotation = physicBody2D.isAllowRotation();

            bodyDef.userData = physicBody2D.gameObject;

            bodyDef.bullet = physicBody2D.isNoneStopCollision();

            bodyDef.gravityScale = physicBody2D.getGravityScale();

            bodyDef.angularVelocity = physicBody2D.getAngularVelocity();

            switch (physicBody2D.getPhysicBodyType()) {
                case Kinematic -> bodyDef.type = BodyType.KINEMATIC;
                case Static -> bodyDef.type = BodyType.STATIC;
                case Dynamic -> bodyDef.type = BodyType.DYNAMIC;
            }

            Body obj = this.world.createBody(bodyDef);

            obj.m_mass = physicBody2D.getMass();

            physicBody2D.setPhysicBodyRef(obj);

            CircleCollider2D circleCollider2D;
            BoxCollider2D boxCollider2D;
            PillBoxCollider pillBoxCollider;

            if ((circleCollider2D =
                    go.getFirstComponent(CircleCollider2D.class)) != null ) {

                addCircleCollider2D(physicBody2D, circleCollider2D);
            }

            if ((boxCollider2D =
                    go.getFirstComponent(BoxCollider2D.class)) != null) {

                addBoxCollider2D(physicBody2D, boxCollider2D);
            }

            if ((pillBoxCollider =
                    go.getFirstComponent(PillBoxCollider.class)) != null) {
                addPillBoxCollider(physicBody2D, pillBoxCollider);
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
        if (colliderComponent instanceof BoxCollider2D boxCollider2D) {
            Body body = physicBody2D.getPhysicBodyRef();

            if (body == null) return;

            PolygonShape shape = new PolygonShape();
            Vector2f scale = boxCollider2D.getLocalScale();

            Vector2f halfSize = new Vector2f(boxCollider2D.getHalfSize()).mul(0.5f).mul(scale); // Applying correct collider box size

            Vector2f offset = boxCollider2D.getLocalPosition();
            float rotation = boxCollider2D.getLocalRotation();

            shape.setAsBox(halfSize.x, halfSize.y, new Vec2(offset.x, offset.y), Math.toRadians(rotation));

            createFixture(physicBody2D, body, shape);
        }
    }

    public void addCircleCollider2D(PhysicBody2D physicBody2D, Component colliderComponent) {
        if (colliderComponent instanceof CircleCollider2D circleCollider2D) {
            Body body = physicBody2D.getPhysicBodyRef();

            if (body == null) return;

            CircleShape shape = new CircleShape();

            Vector2f scale = circleCollider2D.getLocalScale();
            float scaledRadius = circleCollider2D.getRadius() * ((scale.x + scale.y) / 2.0f);

            shape.setRadius(scaledRadius);

            Vector2f offset = circleCollider2D.getLocalPosition();

            shape.m_p.set(new Vec2(offset.x, offset.y));

            createFixture(physicBody2D, body, shape);
        }
    }

    public void addPillBoxCollider(PhysicBody2D physicBody2D, Component colliderComponent) {
        if (colliderComponent instanceof PillBoxCollider pillBoxCollider) {
            Body body = physicBody2D.getPhysicBodyRef();

            if (body == null) return;

            addBoxCollider2D(physicBody2D, pillBoxCollider.getMidBox());
            addCircleCollider2D(physicBody2D, pillBoxCollider.getHeadCircle());
            addCircleCollider2D(physicBody2D, pillBoxCollider.getFootCircle());
        }


    }

    public void resetCollider(PhysicBody2D physicBody2D, Component colliderObject) {
        Body body = physicBody2D.getPhysicBodyRef();

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
        Vec2 gravity = this.world.getGravity();
        return new Vector2f(gravity.x, gravity.y);
    }
}

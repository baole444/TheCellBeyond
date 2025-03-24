package physic_2d;

import TCB_Field.GameObject;
import TCB_Field.Transform;
import components.Component;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.joml.Vector2f;
import physic_2d.components.FlatPhysicBody;
import physic_2d.components.collider.FlatBoxCollider;
import physic_2d.components.collider.FlatCircleCollider;
import physic_2d.components.collider.PillBoxCollider;

public class FlatPhysic {
    // https://box2d.org Check out this website for reference codes

    private Vec2 gravity = new Vec2(0, -9.80665f);
    private World world = new World(gravity);

    private float physicDt = 0.0f;
    private float physicDtRate = 1.0f / 60.0f; // delta of 60 fps
    private int velocityPassCount = 9;
    private int positionPassCount = 3;

    public FlatPhysic() {
        world.setContactListener(new FlatPhysicContactListener());
    }

    public void add(GameObject go) {
        FlatPhysicBody flatPhysicBody = go.getComponent(FlatPhysicBody.class);

        // Duplicate prevention
        if (flatPhysicBody != null && flatPhysicBody.loadInstObjectBody() == null) {
            Transform transform = go.transform;

            // Define rigid body
            BodyDef objDef = new BodyDef();
            objDef.angle = (float)Math.toRadians(transform.rotate);
            objDef.position.set(transform.position.x, transform.position.y);

            objDef.angularDamping = flatPhysicBody.loadRollResistance();
            objDef.linearDamping = flatPhysicBody.loadTranslateResistance();

            objDef.fixedRotation = flatPhysicBody.isRotatable();

            objDef.userData = flatPhysicBody.gameObject;

            objDef.bullet = flatPhysicBody.isNoneStopCollision();

            objDef.gravityScale = flatPhysicBody.loadGravityScale();

            objDef.angularVelocity = flatPhysicBody.loadAngularVelocity();

            switch (flatPhysicBody.loadObjectClassification()) {
                case Kinematic: objDef.type = BodyType.KINEMATIC;
                break;
                case Static: objDef.type = BodyType.STATIC;
                break;
                case Dynamic: objDef.type = BodyType.DYNAMIC;
                break;
            }
            Body obj = this.world.createBody(objDef);

            obj.m_mass = flatPhysicBody.loadMass();

            flatPhysicBody.setInstObjectBody(obj);

            FlatCircleCollider flatCircleCollider;
            FlatBoxCollider flatBoxCollider;
            PillBoxCollider pillBoxCollider;

            if ((flatCircleCollider =
                    go.getComponent(FlatCircleCollider.class)) != null ) {

                addFlatCircleCollider(flatPhysicBody, flatCircleCollider);
            }

            if ((flatBoxCollider =
                    go.getComponent(FlatBoxCollider.class)) != null) {

                addFlatBoxCollider(flatPhysicBody, flatBoxCollider);
            }

            if ((pillBoxCollider =
                    go.getComponent(PillBoxCollider.class)) != null) {
                addPillBoxCollider(flatPhysicBody, pillBoxCollider);
            }
        }
    }

    public void destroyObject(GameObject go) {
        FlatPhysicBody flatPhysicBody = go.getComponent(FlatPhysicBody.class);
        if (flatPhysicBody != null) {
            if (flatPhysicBody.loadInstObjectBody() != null) {
                world.destroyBody(flatPhysicBody.loadInstObjectBody());
                flatPhysicBody.setInstObjectBody(null);
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

    private void createFixture(FlatPhysicBody flatPhysicBody, Body body, Shape shape) {
        FixtureDef fixtureDef = new FixtureDef();

        fixtureDef.shape = shape;
        fixtureDef.density = 1.0f;

        fixtureDef.friction = flatPhysicBody.loadFriction();

        fixtureDef.userData = flatPhysicBody.gameObject;

        fixtureDef.isSensor = flatPhysicBody.isSensor();

        body.createFixture(fixtureDef);
    }

    public void addFlatBoxCollider(FlatPhysicBody flatPhysicBody, Component colliderComponent) {
        FlatBoxCollider flatBoxCollider = (FlatBoxCollider) colliderComponent;
        Body body = flatPhysicBody.loadInstObjectBody();
        assert body != null : "Instant physical body of Object found.";

        PolygonShape shape = new PolygonShape();

        Vector2f halfSize = new Vector2f(flatBoxCollider.loadHalfSize()).mul(0.5f); // Applying correct collider box size

        Vector2f offset = flatBoxCollider.loadOffset();

        shape.setAsBox(halfSize.x, halfSize.y, new Vec2(offset.x, offset.y), 0);

        createFixture(flatPhysicBody, body, shape);
    }

    public void addFlatCircleCollider(FlatPhysicBody flatPhysicBody, Component colliderComponent) {
        FlatCircleCollider flatCircleCollider = (FlatCircleCollider) colliderComponent;
        Body body = flatPhysicBody.loadInstObjectBody();
        assert body != null : "Instant physical body of Object found.";

        CircleShape shape = new CircleShape();

        shape.setRadius(flatCircleCollider.loadRadius());

        Vec2 offset = new Vec2(flatCircleCollider.loadOffset().x, flatCircleCollider.loadOffset().y);

        shape.m_p.set(offset);

        createFixture(flatPhysicBody, body, shape);
    }

    public void addPillBoxCollider(FlatPhysicBody flatPhysicBody, Component colliderComponent) {
        PillBoxCollider pillBoxCollider = (PillBoxCollider) colliderComponent;
        Body body = flatPhysicBody.loadInstObjectBody();
        assert body != null : "Instant physical body of Object found.";

        addFlatBoxCollider(flatPhysicBody, pillBoxCollider.getMidBox());
        addFlatCircleCollider(flatPhysicBody, pillBoxCollider.getHeadCircle());
        addFlatCircleCollider(flatPhysicBody, pillBoxCollider.getFootCircle());
    }

    public void resetCollider(FlatPhysicBody flatPhysicBody, Component colliderObject) {
        Body body = flatPhysicBody.loadInstObjectBody();

        if (body == null) return;

        int size = fixtureListSize(body);

        for (int i = 0; i < size; i++) {
            body.destroyFixture(body.getFixtureList());
        }

        if (colliderObject instanceof FlatBoxCollider) {
            addFlatBoxCollider(flatPhysicBody, colliderObject);
        }
        else if (colliderObject instanceof  FlatCircleCollider) {
            addFlatCircleCollider(flatPhysicBody, colliderObject);
        }
        else if (colliderObject instanceof PillBoxCollider) {
            addPillBoxCollider(flatPhysicBody, colliderObject);
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

    public void setIsSensor(FlatPhysicBody flatPhysicBody, boolean val) {
        Body body = flatPhysicBody.loadInstObjectBody();
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

    public Vector2f loadGravity() {
        Vec2 gravity = this.world.getGravity();
        return new Vector2f(gravity.x, gravity.y);
    }
}

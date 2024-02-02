package physic_2d;

import TCB_Field.GameObject;
import TCB_Field.Transform;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;
import org.joml.Vector2f;
import physic_2d.components.HardObject;
import physic_2d.components.collider.Collider2D;
import physic_2d.components.collider.ColliderCircle;

public class FlatPhysic {
    // https://box2d.org Check out this website for references code

    private Vec2 gravity = new Vec2(0, -9.80665f);
    private World world = new World(gravity);

    private float physicDt = 0.0f;
    private float physicDtRate = 1.0f / 60.0f; // detal of 60 fps
    private int velocityPassCount = 9;
    private int positionPassCount = 3;

    public void add(GameObject go) {
        HardObject hardObject = go.getComponent(HardObject.class);
        // Duplicate prevention
        if (hardObject != null && hardObject.loadInstObject() == null) {
            Transform transform = go.transform;

            BodyDef objDef = new BodyDef();
            objDef.angle = (float)Math.toRadians(transform.rotate);
            objDef.position.set(transform.position.x, transform.position.y);
            objDef.angularDamping = hardObject.loadRollResist();
            objDef.linearDamping = hardObject.loadTranslateResist();
            objDef.fixedRotation = hardObject.isRotatable();
            objDef.bullet = hardObject.isNoneStopCollision();

            switch (hardObject.loadObjectClassification()) {
                case Kinematic: objDef.type = BodyType.KINEMATIC;
                break;
                case Static: objDef.type = BodyType.STATIC;
                break;
                case Dynamic: objDef.type = BodyType.DYNAMIC;
                break;
            }

            PolygonShape shape = new PolygonShape();
            ColliderCircle colliderCircle;
            Collider2D collider2D;

            if ((colliderCircle = go.getComponent(ColliderCircle.class)) != null ) {
                shape.setRadius(colliderCircle.loadRadius());
            } else if ((collider2D = go.getComponent(Collider2D.class)) != null) {
                Vector2f halfSize = new Vector2f(collider2D.loadHalfSize()).mul(0.5f);
                Vector2f offset = collider2D.loadOffset();
                Vector2f origin = new Vector2f(collider2D.loadOrigin());
                shape.setAsBox(halfSize.x, halfSize.y, new Vec2(origin.x, origin.y), 0);

                Vec2 pos = objDef.position;
                float xPos = pos.x + offset.x;
                float yPos = pos.y + offset.y;
                objDef.position.set(xPos, yPos);
            }
            Body obj = this.world.createBody(objDef);

            hardObject.setInstObject(obj);

            obj.createFixture(shape, hardObject.loadMass());
        }
    }

    public void destroyGameObj(GameObject obj) {
        HardObject hObj = obj.getComponent(HardObject.class);

        if (hObj != null) {
            if (hObj.loadInstObject() != null) {
                world.destroyBody(hObj.loadInstObject());
                hObj.setInstObject(null);
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
}

package physic2d.components;

import TheCellBeyond.Window;
import components.Component;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.joml.Math;
import org.joml.Vector2f;
import physic2d.enums.PhysicBodyType;

public class PhysicBody2D extends Component {
    private Vector2f velocity = new Vector2f();

    private float rollResistance = 0.8f;
    private float translateResistance = 0.9f;

    private float friction = 0.0f;

    private float angularVelocity = 0.0f;
    private float gravityScale = 1.0f;

    /**
     * Indicate if an object is a sensor or not.
     * Sensor is a dynamic object in the world
     * that has no physical reaction.
     * It can still trigger collision callback.
     */
    private boolean isSensor = false;

    private float mass = 0;
    private PhysicBodyType physicBodyType = PhysicBodyType.Dynamic;

    private boolean isRotatable = false;

    private boolean isNoneStopCollision = true;

    /**
     * Physical body of the Object.
     */
    private transient Body instObjectBody = null; // Raw Object, a memory reference for physic and game engine update layer.

    @Override
    public void update(float dt) {
        // Sync object between physic engine and game engine
        if (instObjectBody != null) {
            this.gameObject.transform.position.set(
                    instObjectBody.getPosition().x, instObjectBody.getPosition().y
            );

            this.gameObject.transform.rotation = Math.toDegrees(instObjectBody.getAngle());
        }
    }
    public float getFriction() {
        return this.friction;
    }

    public float getAngularVelocity() {
        return this.angularVelocity;
    }

    public float getGravityScale() {
        return this.gravityScale;
    }

    public boolean isSensor() {
        return isSensor;
    }

    public void setFriction(float friction) {
        this.friction = friction;
    }

    public void setAngularVelocity(float angularVelocity) {
        this.angularVelocity = angularVelocity;
        if (instObjectBody != null) {
            this.instObjectBody.setAngularVelocity(angularVelocity);
        }
    }

    public void setGravityScale(float gravityScale) {
        this.gravityScale = gravityScale;
        if (instObjectBody != null) {
            this.instObjectBody.setGravityScale(gravityScale);
        }
    }

    public void setSensor(boolean sensor) {
        isSensor = sensor;
        if (instObjectBody != null) {
            Window.getPhysic2D().setIsSensor(this, sensor);
        }
    }

    public Vector2f getVelocity() {
        return velocity;
    }

    public void addVelocity(Vector2f force) {
        if (instObjectBody != null) {
            instObjectBody.applyForceToCenter(new Vec2(force.x, force.y));
        }
    }

    public void addImpulse(Vector2f impulse) {
        if (instObjectBody != null) {
            instObjectBody.applyLinearImpulse(new Vec2(impulse.x, impulse.y), instObjectBody.getWorldCenter());
        }
    }

    public void setVelocity(Vector2f velocity) {
        this.velocity = velocity;
        if (instObjectBody != null) {
            this.instObjectBody.setLinearVelocity(new Vec2(velocity.x, velocity.y));
        }
    }

    public float getRollResistance() {
        return rollResistance;
    }

    public void setRollResistance(float rollResistance) {
        this.rollResistance = rollResistance;
    }

    public float getTranslateResistance() {
        return translateResistance;
    }

    public void setTranslateResistance(float translateResistance) {
        this.translateResistance = translateResistance;
    }

    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public PhysicBodyType getPhysicBodyType() {
        return physicBodyType;
    }

    public void setPhysicBodyType(PhysicBodyType physicBodyType) {
        this.physicBodyType = physicBodyType;
    }

    public boolean isRotatable() {
        return isRotatable;
    }

    public void setRotatable(boolean rotatable) {
        isRotatable = rotatable;
    }

    public boolean isNoneStopCollision() {
        return isNoneStopCollision;
    }

    public void setNoneStopCollision(boolean noneStopCollision) {
        isNoneStopCollision = noneStopCollision;
    }

    public Body getInstObjectBody() {
        return instObjectBody;
    }

    public void setInstObjectBody(Body instObjectBody) {
        this.instObjectBody = instObjectBody;
    }
}

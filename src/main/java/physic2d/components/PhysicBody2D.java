package physic2d.components;

import TheCellBeyond.Window;
import components.SpatialComponent;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.joml.Math;
import org.joml.Vector2f;
import physic2d.enums.PhysicBodyType;

public class PhysicBody2D extends SpatialComponent {
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

    private boolean allowRotation = false;

    private boolean isNoneStopCollision = true;

    /**
     * Physical body of the Object.
     */
    private transient Body physicBodyRef = null; // Raw Object, a memory reference for physic and game engine update layer.


    @Override
    public void update(float dt) {
        // Sync object between physic engine and game engine
        if (physicBodyRef != null) {
            Vector2f physicPos = new Vector2f(physicBodyRef.getPosition().x, physicBodyRef.getPosition().y);
            float physicRot = Math.toDegrees(physicBodyRef.getAngle());

            setWorldPosition(physicPos);
            setWorldRotation(physicRot);
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
        if (physicBodyRef != null) {
            this.physicBodyRef.setAngularVelocity(angularVelocity);
        }
    }

    public void setGravityScale(float gravityScale) {
        this.gravityScale = gravityScale;
        if (physicBodyRef != null) {
            this.physicBodyRef.setGravityScale(gravityScale);
        }
    }

    public void setSensor(boolean sensor) {
        isSensor = sensor;
        if (physicBodyRef != null) {
            Window.getPhysic2D().setIsSensor(this, sensor);
        }
    }

    public Vector2f getVelocity() {
        return velocity;
    }

    public void addVelocity(Vector2f force) {
        if (physicBodyRef != null) {
            physicBodyRef.applyForceToCenter(new Vec2(force.x, force.y));
        }
    }

    public void addImpulse(Vector2f impulse) {
        if (physicBodyRef != null) {
            physicBodyRef.applyLinearImpulse(new Vec2(impulse.x, impulse.y), physicBodyRef.getWorldCenter());
        }
    }

    public void setVelocity(Vector2f velocity) {
        this.velocity = velocity;
        if (physicBodyRef != null) {
            this.physicBodyRef.setLinearVelocity(new Vec2(velocity.x, velocity.y));
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

    public boolean isAllowRotation() {
        return allowRotation;
    }

    public void setAllowRotation(boolean allowRotation) {
        this.allowRotation = allowRotation;
    }

    public boolean isNoneStopCollision() {
        return isNoneStopCollision;
    }

    public void setNoneStopCollision(boolean noneStopCollision) {
        isNoneStopCollision = noneStopCollision;
    }

    public Body getPhysicBodyRef() {
        return physicBodyRef;
    }

    public void setPhysicBodyRef(Body physicBodyRef) {
        this.physicBodyRef = physicBodyRef;

        if (physicBodyRef != null) {
            Vector2f currentPos = getWorldPosition();
            float currentRot = getRotation();

            this.physicBodyRef.setTransform(new Vec2(currentPos.x, currentPos.y), Math.toRadians(currentRot));
        }
    }
}

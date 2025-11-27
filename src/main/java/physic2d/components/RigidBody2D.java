package physic2d.components;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyDef;
import org.joml.Vector2f;
import physic2d.enums.PhysicBodyType;

public class RigidBody2D extends PhysicBody2D {
    private Vector2f velocity = new Vector2f();
    private float rollResistance = 0.8f;
    private float translateResistance = 0.9f;
    private float angularVelocity = 0.0f;
    private float gravityScale = 1.0f;
    private float mass = 0;

    private boolean allowRotation = false;
    private boolean isNoneStopCollision = true;

    public RigidBody2D() {
        super(PhysicBodyType.Dynamic);
    }

    public RigidBody2D(PhysicBodyType bodyType) {
        super(bodyType);
    }

    @Override
    public void configureBodyDef(BodyDef bodyDef) {
        bodyDef.angularDamping = rollResistance;
        bodyDef.linearDamping = translateResistance;
        bodyDef.fixedRotation = allowRotation;
        bodyDef.bullet = isNoneStopCollision;
        bodyDef.gravityScale = gravityScale;
        bodyDef.angularVelocity = angularVelocity;
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
            physicBodyRef.setLinearVelocity(new Vec2(velocity.x, velocity.y));
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

    public float getAngularVelocity() {
        return angularVelocity;
    }

    public void setAngularVelocity(float angularVelocity) {
        this.angularVelocity = angularVelocity;
        if (physicBodyRef != null) {
            physicBodyRef.setAngularVelocity(angularVelocity);
        }
    }

    public float getGravityScale() {
        return gravityScale;
    }

    public void setGravityScale(float gravityScale) {
        this.gravityScale = gravityScale;
        if (physicBodyRef != null) {
            physicBodyRef.setGravityScale(gravityScale);
        }
    }

    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = mass;
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
}

package physic2d.components;

import TheCellBeyond.Window;
import components.SpatialComponent;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.joml.Math;
import org.joml.Vector2f;
import physic2d.Physic2D;
import physic2d.PhysicLayer;
import physic2d.enums.PhysicBodyType;

public class PhysicBody2D extends SpatialComponent {
    private PhysicBodyType physicBodyType = PhysicBodyType.Dynamic;
    private Vector2f velocity = new Vector2f();
    private float rollResistance = 0.8f;
    private float translateResistance = 0.9f;
    private float friction = 0.0f;
    private float angularVelocity = 0.0f;
    private float gravityScale = 1.0f;
    private float mass = 0;
    private int collisionLayer = PhysicLayer.layerToBit(0);
    private int collisionMask = PhysicLayer.layerToBit(0);

    private boolean isSensor = false;
    private boolean allowRotation = false;
    private boolean isNoneStopCollision = true;
    private transient Body physicBodyRef = null;

    private transient boolean needFixtureUpdate = false;

    @Override
    public void update(float dt) {
        if (physicBodyRef == null) {
            additionalUpdateLogic(dt);
            return;
        }

        Vector2f physicPos = new Vector2f(physicBodyRef.getPosition().x, physicBodyRef.getPosition().y);
        float physicRot = Math.toDegrees(physicBodyRef.getAngle());

        setWorldPosition(physicPos);
        setWorldRotation(physicRot);

        if (needFixtureUpdate) updateFixtureFilter();

        additionalUpdateLogic(dt);
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
            physicBodyRef.setGravityScale(gravityScale);
        }
    }

    public void setSensor(boolean sensor) {
        this.isSensor = sensor;
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

    public int getCollisionLayer() {
        return collisionLayer;
    }

    public void addCollisionLayer(int layerIndex) {
        int pastVal = collisionLayer;
        collisionLayer = PhysicLayer.addLayerToMask(collisionLayer, layerIndex);
        if (pastVal != collisionLayer) needFixtureUpdate = true;
    }

    public void removeCollisionLayer(int layerIndex) {
        int pastVal = collisionLayer;
        collisionLayer = PhysicLayer.removeLayerFromMask(collisionLayer, layerIndex);
        if (pastVal != collisionLayer) needFixtureUpdate = true;
    }

    public int getCollisionMask() {
        return collisionMask;
    }

    public void addCollisionMask(int layerIndex) {
        int pastVal = collisionMask;
        collisionMask = PhysicLayer.addLayerToMask(collisionMask, layerIndex);
        if (pastVal != collisionMask) needFixtureUpdate = true;
    }

    public void removeCollisionMask(int layerIndex) {
        int pastVal = collisionMask;
        collisionMask = PhysicLayer.removeLayerFromMask(collisionMask, layerIndex);
        if (pastVal != collisionMask) needFixtureUpdate = true;
    }

    private void updateFixtureFilter() {
        if (!needFixtureUpdate) return;

        Physic2D physic2D = Window.getPhysic2D();
        if (physic2D == null || physic2D.isLock()) return;
        if (physicBodyRef == null) {
            needFixtureUpdate = false;
            return;
        }

        physic2D.updateBodyFilters(this);
        needFixtureUpdate = false;
    }
}

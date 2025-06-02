package threading.states;

import org.joml.Vector2f;

import java.util.Objects;

public final class PhysicsBodyState {
    private int objectId;
    private Vector2f position;
    private Vector2f velocity;
    private float angularVelocity;
    private float angle;

    public PhysicsBodyState() {}

    public PhysicsBodyState(int objectId, Vector2f position, Vector2f velocity, float angularVelocity, float angle) {
        this.objectId = objectId;
        this.position = position;
        this.velocity = velocity;
        this.angularVelocity = angularVelocity;
        this.angle = angle;
    }

    public int getObjectId() {
        return objectId;
    }

    public PhysicsBodyState setObjectId(int objectId) {
        this.objectId = objectId;
        return this;
    }

    public Vector2f getPosition() {
        return position;
    }

    public PhysicsBodyState setPosition(Vector2f position) {
        this.position = position;
        return this;
    }

    public Vector2f getVelocity() {
        return velocity;
    }

    public PhysicsBodyState setVelocity(Vector2f velocity) {
        this.velocity = velocity;
        return this;
    }

    public float getAngularVelocity() {
        return angularVelocity;
    }

    public PhysicsBodyState setAngularVelocity(float angularVelocity) {
        this.angularVelocity = angularVelocity;
        return this;
    }

    public float getAngle() {
        return angle;
    }

    public PhysicsBodyState setAngle(float angle) {
        this.angle = angle;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PhysicsBodyState) obj;
        return this.objectId == that.objectId &&
                Objects.equals(this.position, that.position) &&
                Objects.equals(this.velocity, that.velocity) &&
                Float.floatToIntBits(this.angularVelocity) == Float.floatToIntBits(that.angularVelocity) &&
                Float.floatToIntBits(this.angle) == Float.floatToIntBits(that.angle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectId, position, velocity, angularVelocity, angle);
    }

    @Override
    public String toString() {
        return "PhysicsBodyState[" +
                "objectId=" + objectId + ", " +
                "position=" + position + ", " +
                "velocity=" + velocity + ", " +
                "angularVelocity=" + angularVelocity + ", " +
                "angle=" + angle + ']';
    }

}

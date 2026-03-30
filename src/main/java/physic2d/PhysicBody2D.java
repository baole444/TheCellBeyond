package physic2d;

import org.jbox2d.dynamics.BodyType;
import org.joml.Vector2f;
import physic2d.enums.PhysicBodyType;

/**
 * PhysicBody2D is abstract base class for 2D game objects affected by physics.
 * All 2D physics bodies inherit from it.
 */
public abstract class PhysicBody2D extends CollisionObject2D {
    protected PhysicBodyType physicBodyType;
    protected float friction = 0.0f;

    /**
     * Create new {@link PhysicBody2D} of the given body type.
     * @param bodyType the body type for the physic body
     */
    public PhysicBody2D(PhysicBodyType bodyType) {
        String name = PhysicBody2D.class.getSimpleName();
        this(name, bodyType);
    }

    /**
     * Create a new {@link PhysicBody2D} of the given body type with the given name.
     * @param name the new name for the physic body
     * @param bodyType the body type for the physic body
     */
    public PhysicBody2D(String name, PhysicBodyType bodyType) {
        if (invalidName(name)) name = PhysicBody2D.class.getSimpleName();
        super(name);
        physicBodyType = bodyType;
    }

    @Override
    public float friction() {
        return friction;
    }

    @Override
    public BodyType bodyType() {
        return switch (physicBodyType) {
            case Kinematic -> BodyType.KINEMATIC;
            case Static -> BodyType.STATIC;
            case Dynamic -> BodyType.DYNAMIC;
        };
    }

    /**
     * Set the friction ratio for this physic body.
     * @param friction the friction ratio
     */
    public void friction(float friction) {
        this.friction = friction;
    }

    /**
     * Get the body type of this physic body.
     * @return the {@link PhysicBodyType} of this body
     */
    public PhysicBodyType getPhysicBodyType() {
        return physicBodyType;
    }

    /**
     * Set the body type for this physic body.
     * @param physicBodyType the physic body type to set
     */
    public void setPhysicBodyType(PhysicBodyType physicBodyType) {
        if (physicBodyType == null) return;
        this.physicBodyType = physicBodyType;
    }

    /**
     * Add movement velocity to this physic body using the given vector.
     * Depends on the body type, the values might be processed differently.
     * @param velocity the velocity vector to add (unit: m/s)
     */
    public void addMovement(Vector2f velocity) {}

    /**
     * Reset movement velocity of this physic body.
     * Depends on the body type, this might have different behaviour.
     */
    public void resetMovement() {}

    /**
     * Ger the current linear velocity of this physic body.
     * @return the current linear velocity vector (unit: m/s)
     */
    public Vector2f linearVelocity() {
        return new Vector2f();
    }
}

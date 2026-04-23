package physic2d;

import org.jbox2d.collision.shapes.MassData;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyDef;
import org.joml.Vector2f;
import physic2d.enums.PhysicBodyType;

/**
 * RigidBody2D is a fully simulated physic body. It cannot be controlled directly, instead, forces (gravity, impulses, etc.)
 * are applied for physic simulation to calculate the resulting movement, rotation and react to collisions.
 * This allows the body to affect other physic bodies in its path.
 */
public class RigidBody2D extends PhysicBody2D {
    private final Vector2f initialVelocity = new Vector2f();
    private float rollResistance = 0.8f;
    private float translateResistance = 0.8f;
    private float angularVelocity = 0.0f;
    private float gravityScale = 1.0f;
    private float mass = 0.1f;
    private boolean fixedRotation = false;
    private boolean bullet = true;
    private transient final Vector2f currentVelocity = new Vector2f();

    /**
     * Create a new dynamic {@link RigidBody2D}.
     */
    public RigidBody2D() {
        String name = RigidBody2D.class.getSimpleName();
        this(name);
    }

    /**
     * Create a new dynamic {@link RigidBody2D} with the given name.
     * @param name the new name for the object
     */
    public RigidBody2D(String name) {
        this(name, PhysicBodyType.Dynamic);
    }

    /**
     * Create a new {@link RigidBody2D} with the given body type.
     * If the given type is null, it will default to dynamic.
     * @param bodyType the type physic body for the object
     */
    public RigidBody2D(PhysicBodyType bodyType) {
        String name = RigidBody2D.class.getSimpleName();
        this(name, bodyType);
    }

    /**
     * Create a new {@link RigidBody2D} with the given name and body type.
     * @param name the new name for the object
     * @param bodyType the type physic body for the object
     */
    public RigidBody2D(String name, PhysicBodyType bodyType) {
        if (invalidName(name)) name = RigidBody2D.class.getSimpleName();
        if (bodyType == null) bodyType = PhysicBodyType.Dynamic;
        super(name, bodyType);
    }

    @Override
    public void configureBodyDef(BodyDef bodyDef) {
        bodyDef.angularDamping = rollResistance;
        bodyDef.linearDamping = translateResistance;
        bodyDef.fixedRotation = fixedRotation;
        bodyDef.bullet = bullet;
        bodyDef.gravityScale = gravityScale;
        bodyDef.angularVelocity = (float) Math.toRadians(angularVelocity);
    }

    @Override
    public void configurePhysicBodyRef() {
        if (physicBodyRef == null) return;
        mass(mass);
        physicBodyRef.setLinearVelocity(new Vec2(initialVelocity.x, initialVelocity.y));
    }

    @Override
    protected void onPhysicUpdate(float dt) {
        if (physicBodyRef == null) return;
        Vec2 v = physicBodyRef.getLinearVelocity();
        currentVelocity.set(v.x, v.y);
        angularVelocity = (float) Math.toDegrees(physicBodyRef.getAngularVelocity());
    }

    @Override
    public Vector2f linearVelocity() {
        return new Vector2f(currentVelocity);
    }

    /**
     * Add force to the centre by converting the movement vector
     * @param velocity the velocity vector to add, in m/s
     */
    @Override
    public void addMovement(Vector2f velocity) {
        if (physicBodyRef == null) return;
        float mFactor = mass > 0.0f ? mass : 1.0f;
        Vector2f force = new Vector2f(velocity).mul(mFactor);
        addForceToCenter(force);
    }

    /**
     * Get the initial velocity of this rigid body when it is first added to te scene.
     * This velocity is applied only once in the body's life cycle.
     * @return a copy of the initial velocity vector
     */
    public Vector2f initialVelocity() {
        return new Vector2f(initialVelocity);
    }

    /**
     * Set the initial velocity for this rigid body using the given velocity.
     * If this is called during simulation, it will override the current linear velocity with the given initial velocity.
     * @param initialVelocity the velocity vector to apply
     */
    public void initialVelocity(Vector2f initialVelocity) {
        if (initialVelocity == null) return;
        this.initialVelocity.set(initialVelocity);
        if (physicBodyRef != null) physicBodyRef.setLinearVelocity(new Vec2(initialVelocity.x, initialVelocity.y));
    }

    /**
     * Apply force to the centre of the rigid body's mass.
     * @param force the force vector to apply, in Newtons
     */
    public void addForceToCenter(Vector2f force) {
        if (physicBodyRef != null) physicBodyRef.applyForceToCenter(new Vec2(force.x, force.y));
    }

    /**
     * Apply linear impulse to the centre of the rigid body's mass.
     * @param impulse the impulse vector to apply, in N*s (Newton--seconds)
     */
    public void addImpulse(Vector2f impulse) {
        if (physicBodyRef != null) physicBodyRef.applyLinearImpulse(new Vec2(impulse.x, impulse.y), physicBodyRef.getWorldCenter());
    }

    /**
     * Get the angular damping of the rigid body, which determine the resistance against rotation.
     * @return the angular damping decay coefficient
     */
    public float rollResistance() {
        return rollResistance;
    }

    /**
     * Set the angular damping to determine how hard it is to roll the body.
     * Higher damping value will result in faster stop when rolling.
     * <p>
     * Value passed in will be make absolute (no negative) to prevent undefined behaviour.
     * @param coefficient the angular damping decay coefficient
     * @see #fixedRotation(boolean) Set the rotation lock on rigid body
     */
    public void rollResistance(float coefficient) {
        coefficient = Math.abs(coefficient);
        this.rollResistance = coefficient;
        if (physicBodyRef != null) physicBodyRef.setAngularDamping(coefficient);
    }

    /**
     * Get the linear damping of the rigid body, which determine the resistance against linear movement.
     * @return the linear damping decay coefficient
     */
    public float moveResistance() {
        return translateResistance;
    }

    /**
     * Set the linear damping to determine how hard it is to move the body.
     * Higher damping value will result in faster stop when moving.
     * <p>
     * Value passed in will be make absolute (no negative) to prevent undefined behaviour.
     * @param coefficient the linear damping decay coefficient
     */
    public void moveResistance(float coefficient) {
        coefficient = Math.abs(coefficient);
        this.translateResistance = coefficient;
        if (physicBodyRef != null) physicBodyRef.setLinearDamping(coefficient);
    }

    /**
     * Get the angular velocity of this rigid body.
     * @return the angular velocity, in degrees / sec
     */
    public float angularVelocity() {
        return angularVelocity;
    }

    /**
     * Set the angular velocity for this rigid body.
     * If this is called during simulation, it will override the existing angular velocity with the given degrees
     * @param degrees the angular velocity value, in degrees / sec
     */
    public void angularVelocity(float degrees) {
        this.angularVelocity = degrees;
        if (physicBodyRef != null) physicBodyRef.setAngularVelocity((float) Math.toRadians(degrees));
    }

    /**
     * Get the gravity scale multiplier of this rigid body.
     * @return the gravity scale
     */
    public float gravityScale() {
        return gravityScale;
    }

    /**
     * Set the gravity scale multiplier for this rigid body.
     * At 0.0, gravity is disabled for this body and negative value will invert the gravity direction
     * @param gravityScale the gravity scale multiplier
     */
    public void gravityScale(float gravityScale) {
        this.gravityScale = gravityScale;
        if (physicBodyRef != null) physicBodyRef.setGravityScale(gravityScale);
    }

    /**
     * Get the mass of this rigid body.
     * @return the mass, in kg
     */
    public float mass() {
        return mass;
    }

    /**
     * Set the mass for this rigid body. Value passes in will be clamped to minimum of 0.001 kg of mass.
     * @param mass the weight of the body, in kg
     */
    public void mass(float mass) {
        mass = Math.max(0.001f, mass);
        this.mass = mass;
        if (physicBodyRef == null) return;
        MassData massData = new MassData();
        physicBodyRef.getMassData(massData);
        massData.mass = mass;
        physicBodyRef.setMassData(massData);
    }

    /**
     * Check if rotation lock (fixed rotation) of this rigid body is enabled or not.
     * If enabled, this body will no longer rotate when apply angular velocity.
     * @return true if rotation is locked
     */
    public boolean fixedRotation() {
        return fixedRotation;
    }

    /**
     * Set the rotation lock state (fixed rotation) for this rigid body.
     * @param fixedRotation true to lock the object
     */
    public void fixedRotation(boolean fixedRotation) {
        this.fixedRotation = fixedRotation;
        if (physicBodyRef != null) physicBodyRef.setFixedRotation(fixedRotation);
    }

    /**
     * Check if continuous collision detection (bullet mode) is enabled for this rigid body.
     * This to help with reducing tunnelling on fast moving objects.
     * @return true if bullet mode is enabled
     */
    public boolean bullet() {
        return bullet;
    }

    /**
     * Set the continuous collision detection (bullet mode) for this rigid body.
     * @param bullet true to enable bullet mode
     */
    public void bullet(boolean bullet) {
        this.bullet = bullet;
        if (physicBodyRef != null) physicBodyRef.setBullet(bullet);
    }

    @Override
    public RigidBody2D copy() {
        return copy(false);
    }

    @Override
    public RigidBody2D copy(boolean copyHierarchy) {
        RigidBody2D copy = (RigidBody2D) copySingleObject();
        if (copyHierarchy && !getChildren().isEmpty()) copyDescendants(this, copy);
        return copy;
    }
}

package physic2d;

import org.jbox2d.collision.shapes.MassData;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyDef;
import org.joml.Vector2f;
import physic2d.enums.PhysicBodyType;

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

    public RigidBody2D() {
        String name = RigidBody2D.class.getSimpleName();
        this(name);
    }

    public RigidBody2D(String name) {
        this(name, PhysicBodyType.Dynamic);
    }

    public RigidBody2D(PhysicBodyType bodyType) {
        String name = RigidBody2D.class.getSimpleName();
        this(name, bodyType);
    }

    public RigidBody2D(String name, PhysicBodyType bodyType) {
        if (invalidName(name)) name = RigidBody2D.class.getSimpleName();
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
     * Add force to the center by converting the movement vector
     * @param velocity the velocity vector to add (unit: m/s)
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

    public void addForceToCenter(Vector2f force) {
        if (physicBodyRef != null) physicBodyRef.applyForceToCenter(new Vec2(force.x, force.y));
    }

    public void addImpulse(Vector2f impulse) {
        if (physicBodyRef != null) physicBodyRef.applyLinearImpulse(new Vec2(impulse.x, impulse.y), physicBodyRef.getWorldCenter());
    }

    public float rollResistance() {
        return rollResistance;
    }

    public void rollResistance(float rollResistance) {
        this.rollResistance = rollResistance;
        if (physicBodyRef != null) physicBodyRef.setAngularDamping(rollResistance);
    }

    public float translateResistance() {
        return translateResistance;
    }

    public void translateResistance(float translateResistance) {
        this.translateResistance = translateResistance;
        if (physicBodyRef != null) physicBodyRef.setLinearDamping(translateResistance);
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

    public float gravityScale() {
        return gravityScale;
    }

    public void gravityScale(float gravityScale) {
        this.gravityScale = gravityScale;
        if (physicBodyRef != null) physicBodyRef.setGravityScale(gravityScale);
    }

    public float mass() {
        return mass;
    }

    public void mass(float mass) {
        mass = Math.max(0.001f, mass);
        this.mass = mass;
        if (physicBodyRef == null) return;
        MassData massData = new MassData();
        physicBodyRef.getMassData(massData);
        massData.mass = mass;
        physicBodyRef.setMassData(massData);
    }

    public boolean fixedRotation() {
        return fixedRotation;
    }

    public void fixedRotation(boolean fixedRotation) {
        this.fixedRotation = fixedRotation;
        if (physicBodyRef != null) physicBodyRef.setFixedRotation(fixedRotation);
    }

    public boolean bullet() {
        return bullet;
    }

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

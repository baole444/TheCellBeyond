package physic2d;

import editor.ImEditorGui;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
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
        this(PhysicBodyType.Dynamic);
    }

    public RigidBody2D(String name) {
        this (name, PhysicBodyType.Dynamic);
    }

    public RigidBody2D(PhysicBodyType bodyType) {
        String name = RigidBody2D.class.getSimpleName();
        this(name, bodyType);
    }

    public RigidBody2D(String name, PhysicBodyType bodyType) {
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
    public void configureBody() {
        if (physicBodyRef == null) return;
        setMass(mass);
        physicBodyRef.setLinearVelocity(new Vec2(initialVelocity.x, initialVelocity.y));
    }

    @Override
    protected void physicUpdate(float dt) {
        if (physicBodyRef == null) return;
        Vec2 v = physicBodyRef.getLinearVelocity();
        currentVelocity.set(v.x, v.y);
        angularVelocity = (float) Math.toDegrees(physicBodyRef.getAngularVelocity());
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

    public Vector2f getInitialVelocity() {
        return new Vector2f(initialVelocity);
    }

    public void addForceToCenter(Vector2f force) {
        if (physicBodyRef != null) {
            physicBodyRef.applyForceToCenter(new Vec2(force.x, force.y));
        }
    }

    public void addImpulse(Vector2f impulse) {
        if (physicBodyRef != null) {
            physicBodyRef.applyLinearImpulse(new Vec2(impulse.x, impulse.y), physicBodyRef.getWorldCenter());
        }
    }

    public void setInitialVelocity(Vector2f initialVelocity) {
        if (initialVelocity == null) return;
        this.initialVelocity.set(initialVelocity);
        if (physicBodyRef != null) physicBodyRef.setLinearVelocity(new Vec2(initialVelocity.x, initialVelocity.y));
    }

    public float getRollResistance() {
        return rollResistance;
    }

    public void setRollResistance(float rollResistance) {
        this.rollResistance = rollResistance;
        if (physicBodyRef != null) physicBodyRef.setAngularDamping(rollResistance);
    }

    public float getTranslateResistance() {
        return translateResistance;
    }

    public void setTranslateResistance(float translateResistance) {
        this.translateResistance = translateResistance;
        if (physicBodyRef != null) physicBodyRef.setLinearDamping(translateResistance);
    }

    public float getAngularVelocity() {
        return angularVelocity;
    }

    public void setAngularVelocity(float degree) {
        this.angularVelocity = degree;
        if (physicBodyRef != null) physicBodyRef.setAngularVelocity((float) Math.toRadians(degree));
    }

    public float getGravityScale() {
        return gravityScale;
    }

    public void setGravityScale(float gravityScale) {
        this.gravityScale = gravityScale;
        if (physicBodyRef != null) physicBodyRef.setGravityScale(gravityScale);
    }

    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        mass = Math.max(0.001f, mass);
        this.mass = mass;
        if (physicBodyRef != null) {
            MassData massData = new MassData();
            physicBodyRef.getMassData(massData);
            massData.mass = mass;
            physicBodyRef.setMassData(massData);
        }
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

    @Override
    protected void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openRigid = ImGui.collapsingHeader("RigidBody2D##RigidBody2D_Properties_Header", ImGuiTreeNodeFlags.DefaultOpen);
        if (!openRigid) {
            super.additionalImGuiLogic();
            return;
        }
        ImGui.indent();
        Vector2f vTmp = new Vector2f(initialVelocity);
        boolean vChanged = ImEditorGui.dragVec2Ctrl("Velocity", vTmp, 0.0f, this);
        float angularV = ImEditorGui.dragFloatCtrl("Angular Velocity", angularVelocity, 0.0f, 1.0f,this);
        float ms = ImEditorGui.dragFloatCtrl("Mass", mass, 0.0f, this, 0.0f);
        float rollResist = ImEditorGui.dragFloatCtrl("Roll Resistance", rollResistance, 0.8f, this, 0.0f);
        float translateResist = ImEditorGui.dragFloatCtrl("Translate Resistance", translateResistance, 0.8f, this, 0.0f);
        float gravScale = ImEditorGui.dragFloatCtrl("Gravity Scale", gravityScale, 1.0f, this);
        ImBoolean fixedRot = new ImBoolean(fixedRotation);
        if (ImGui.checkbox("Fixed Rotation##RigidBody2D_fixedRotation_" + getUUID(), fixedRot)) fixedRotation(fixedRot.get());
        ImBoolean b = new ImBoolean(bullet);
        if (ImGui.checkbox("Bullet##RigidBody2D_bullet_" + getUUID(), b)) bullet(b.get());

        if (vChanged) setInitialVelocity(vTmp);
        if (Float.compare(angularV, angularVelocity) != 0) setAngularVelocity(angularV);
        if (Float.compare(ms, mass) != 0) setMass(ms);
        if (Float.compare(rollResist, rollResistance) != 0) setRollResistance(rollResist);
        if (Float.compare(translateResist, translateResistance) != 0) setTranslateResistance(translateResist);
        if (Float.compare(gravScale, gravityScale) != 0) setGravityScale(gravScale);

        ImGui.unindent();
        super.additionalImGuiLogic();
    }
}

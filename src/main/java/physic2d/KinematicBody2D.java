package physic2d;

import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyDef;
import org.joml.Vector2f;
import physic2d.enums.PhysicBodyType;

public class KinematicBody2D extends PhysicBody2D {
    private final Vector2f velocity = new Vector2f();
    private float angularVelocity = 0.0f;
    private boolean fixedRotation = false;
    private boolean bullet = false;

    public KinematicBody2D() {
        String name = KinematicBody2D.class.getSimpleName();
        this(name);
    }

    public KinematicBody2D(String name) {
        if (invalidName(name)) name = KinematicBody2D.class.getSimpleName();
        super(name, PhysicBodyType.Kinematic);
    }

    @Override
    public void configureBodyDef(BodyDef bodyDef) {
        bodyDef.fixedRotation = fixedRotation;
        bodyDef.bullet = bullet;
    }

    @Override
    public void configureBody() {
        if (physicBodyRef == null) return;
        physicBodyRef.setLinearVelocity(new Vec2(velocity.x, velocity.y));
        physicBodyRef.setAngularVelocity((float) Math.toRadians(angularVelocity));
    }

    @Override
    protected void onPhysicUpdate(float dt) {
        if (physicBodyRef == null) return;
        angularVelocity = (float) Math.toDegrees(physicBodyRef.getAngularVelocity());
    }

    @Override
    public void addMovement(Vector2f velocity) {
        this.velocity.add(velocity);
        updateVelocity();
    }

    public Vector2f velocity() {
        return new Vector2f(velocity);
    }

    public void velocity(Vector2f velocity) {
        if (velocity == null) return;
        this.velocity.set(velocity);
        updateVelocity();
    }

    public void velocity(float x, float y) {
        velocity.set(x, y);
        updateVelocity();
    }

    public float angularVelocity() {
        return angularVelocity;
    }

    public void angularVelocity(float degree) {
        angularVelocity = degree;
        updateAngularVelocity();
    }

    public boolean fixedRotation() {
        return fixedRotation;
    }

    public void fixedRotation(boolean fixedRotation) {
        this.fixedRotation = fixedRotation;
        if (physicBodyRef == null) return;
        physicBodyRef.setFixedRotation(fixedRotation);
    }

    public boolean bullet() {
        return bullet;
    }

    public void bullet(boolean bullet) {
        this.bullet = bullet;
        if (physicBodyRef == null) return;
        physicBodyRef.setBullet(bullet);
    }

    private void updateVelocity() {
        if (physicBodyRef == null) return;
        physicBodyRef.setLinearVelocity(new Vec2(velocity.x, velocity.y));
    }

    private void updateAngularVelocity() {
        if (physicBodyRef == null) return;
        physicBodyRef.setAngularVelocity((float) Math.toRadians(angularVelocity));
    }

    @Override
    public KinematicBody2D copy() {
        return copy(false);
    }

    @Override
    public KinematicBody2D copy(boolean copyHierarchy) {
        KinematicBody2D copy = (KinematicBody2D) copySingleObject();
        if (copyHierarchy && !getChildren().isEmpty()) copyDescendants(this, copy);
        return copy;
    }

    @Override
    public void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openKinematic = ImGui.collapsingHeader("KinematicBody2D##KinematicBody2D_Properties_Header_" + getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openKinematic) {
            super.additionalImGuiLogic();
            return;
        }

        ImGui.indent();
        Vector2f vec = new Vector2f(velocity);
        if (EditorWidget.dragVec2Ctrl("Velocity", vec, 0.0f, this)) velocity(vec);

        float angle = EditorWidget.dragFloatCtrl("Angular velocity", angularVelocity, 0.0f, 1.0f, this);
        if (Float.compare(angle, angularVelocity) != 0) angularVelocity(angle);

        ImGui.spacing();
        ImBoolean rotation = new ImBoolean(fixedRotation);
        if (ImGui.checkbox("Fixed rotation##KinematicBody2D_fixedRotation_CheckBox_" + getUUID(), rotation)) fixedRotation(rotation.get());

        ImBoolean bullet = new ImBoolean(this.bullet);
        if (ImGui.checkbox("Bullet##KinematicBody2D_bullet_CheckBox_" + getUUID(), bullet)) bullet(bullet.get());

        ImGui.unindent();
        super.additionalImGuiLogic();
    }
}

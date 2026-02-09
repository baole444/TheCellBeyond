package physic2d;

import TheCellBeyond.internal.LogicServer;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyDef;
import org.joml.Vector2f;
import physic2d.enums.MotionMode;
import physic2d.enums.PhysicBodyType;

public class CharacterBody2D extends PhysicBody2D {
    private static final int MaxCollisionSlide = 4;

    private final Vector2f UpDirection = new Vector2f(0.0f, 1.0f);
    private float maxSlopeAngle = 45.0f;
    private float floorSnapDistance = 0.1f;
    public boolean snapToFloor = true;

    private MotionMode motionMode = MotionMode.Grounded;
    private float safeMargin = 0.01f;

    public transient final Vector2f velocity = new Vector2f();
    private transient boolean isOnFloor = false;
    private transient boolean isOnWall = false;
    private transient boolean isOnCeiling = false;
    private transient final Vector2f floorNormal = new Vector2f(0.0f, 1.0f);
    private transient final Vector2f wallNormal = new Vector2f();
    private transient int slideCollisionCount = 0;

    public CharacterBody2D() {
        String name = CharacterBody2D.class.getSimpleName();
        this(name);
    }

    public CharacterBody2D(String name) {
        if (invalidName(name)) name = CharacterBody2D.class.getSimpleName();
        super(name, PhysicBodyType.Kinematic);
    }

    @Override
    public void configureBodyDef(BodyDef bodyDef) {
        bodyDef.fixedRotation = true;
        bodyDef.bullet = true;
    }

    @Override
    public void configureBody() {}

    @Override
    protected void onPhysicUpdate(float dt) {
        if (physicBodyRef == null) return;
        resetMotionState();
    }

    @Override
    public void addMovement(Vector2f velocity) {
        velocity.add(velocity);
    }

    public Vector2f upDirection() {
        return new Vector2f(UpDirection);
    }

    public void upDirection(Vector2f upDirection) {
        UpDirection.set(upDirection).normalize();
    }

    public float maxSlopeAngle() {
        return maxSlopeAngle;
    }

    public void maxSlopeAngle(float degrees) {
        maxSlopeAngle = Math.max(0.0f, Math.min(90.0f, degrees));
    }

    public float floorSnapDistance() {
        return floorSnapDistance;
    }

    public void floorSnapDistance(float length) {
        floorSnapDistance = Math.max(0.0f, length);
    }

    public float safeMargin() {
        return safeMargin;
    }

    public void safeMargin(float margin) {
        safeMargin = Math.max(0.001f, margin);
    }

    public MotionMode motionMode() {
        return motionMode;
    }

    public void motionMode(MotionMode mode) {
        if (mode == null || motionMode == mode) return;
        motionMode = mode;
    }

    public boolean isOnFloor() {
        return motionMode == MotionMode.Grounded && isOnFloor;
    }

    public boolean isOnFloorOnly() {
        return motionMode == MotionMode.Grounded && isOnFloor && !isOnWall && !isOnCeiling;
    }

    public boolean isOnWall() {
        return isOnWall;
    }

    public boolean isOnWallOnly() {
        return isOnWall && !isOnFloor && !isOnCeiling;
    }

    public boolean isOnCeiling() {
        return motionMode == MotionMode.Grounded && isOnCeiling;
    }

    public boolean isOnCeilingOnly() {
        return motionMode == MotionMode.Grounded && isOnCeiling && !isOnFloor && !isOnWall;
    }

    public boolean isCollided() {
        return isOnFloor || isOnWall || isOnCeiling;
    }

    public Vector2f floorNormal() {
        return new Vector2f(floorNormal);
    }

    public Vector2f wallNormal() {
        return new Vector2f(wallNormal);
    }

    public Vector2f getCollisionNormal() {
        if (isOnFloor) return floorNormal();
        if (isOnWall) return  wallNormal();
        if (isOnCeiling) return new Vector2f(UpDirection).negate();
        return new Vector2f();
    }

    public boolean moveAndSlide() {
        if (physicBodyRef == null) return false;
        Vector2f motion = new Vector2f(velocity).mul(Physic2D.PhysicDeltaRate);
        slideCollisionCount = 0;

        motion = slideMotion(motion);
        boolean collided = slideCollisionCount > 0;

        if (motion.lengthSquared() > 0.0f) {
            Vec2 v2 = physicBodyRef.getPosition();
            Vector2f currentPos = new Vector2f(v2.x, v2.y);
            Vector2f nextPos = new Vector2f(currentPos).add(motion);
            physicBodyRef.setTransform(new Vec2(nextPos.x, nextPos.y), physicBodyRef.getAngle());
        }

        if (motionMode == MotionMode.Grounded) {
            checkFloorState();
            if (snapToFloor && !isOnFloor && velocity.y <= 0.0f) floorSnap();
        }

        return collided;
    }

    private Vector2f slideMotion(Vector2f motion) {
        if (slideCollisionCount >= MaxCollisionSlide || motion.lengthSquared() < safeMargin * safeMargin) return new Vector2f();

        slideCollisionCount++;
        Vector2f currentPos = globalPosition();
        Vector2f targetPos = new Vector2f(currentPos).add(motion);

        RayCastInfo rayCast = LogicServer.physic2D().rayCastInfo(this, currentPos, targetPos);
        if (!rayCast.hit || rayCast.contactedObject == null) return motion;

        float fraction = rayCast.rayVectorFraction;
        Vector2f hitNormal = rayCast.normalDirection;
        Vector2f safeMotion = new Vector2f(motion).mul(Math.max(0.0f, fraction - safeMargin));
        Vector2f remainMotion = new Vector2f(motion).sub(safeMotion);

        float dot = remainMotion.dot(hitNormal);
        Vector2f slideVector = new Vector2f(remainMotion).sub(new Vector2f(hitNormal).mul(dot));
        handleCollision(hitNormal, slideVector);

        Vector2f nextSlide = slideMotion(slideVector);
        return new Vector2f(safeMotion).add(nextSlide);
    }

    private void handleCollision(Vector2f normal, Vector2f slideVelocity) {
        if (motionMode == MotionMode.Floating) {
            isOnWall = true;
            wallNormal.set(normal);
            return;
        }

        if (motionMode != MotionMode.Grounded) return;
        categorizeCollision(normal);

        if ((isFloorNormal(normal) && velocity.y <= 0.0f) || (isCeilingNormal(normal) && velocity.y >= 0.0f)) {
            velocity.y = 0.0f;
            slideVelocity.y = 0.0f;
        }
    }

    private void categorizeCollision(Vector2f normal) {
        if (motionMode == MotionMode.Floating) {
            isOnWall = true;
            wallNormal.set(normal);
            return;
        }

        float angle = angleInDegree(normal, UpDirection);
        if (angle <= maxSlopeAngle) {
            isOnFloor = true;
            floorNormal.set(normal);
            return;
        }

        if (angle >= 180.0f - maxSlopeAngle) {
            isOnCeiling = true;
            return;
        }

        isOnWall = true;
        wallNormal.set(normal);
    }

    private boolean isFloorNormal(Vector2f normal) {
        if (motionMode != MotionMode.Grounded) return false;
        float angle = angleInDegree(normal, UpDirection);
        return angle <= maxSlopeAngle;
    }

    private boolean isCeilingNormal(Vector2f normal) {
        if (motionMode != MotionMode.Grounded) return false;
        float angle = angleInDegree(normal, UpDirection);
        return angle >= 180.0f - maxSlopeAngle;
    }

    private void checkFloorState() {
        if (motionMode != MotionMode.Grounded) return;
        if (physicBodyRef == null) return;

        Vector2f pos = globalPosition();
        Vector2f safeDistance = new Vector2f(UpDirection).mul(-safeMargin * 2.0f);
        Vector2f targetPos = new Vector2f(pos).add(safeDistance);

        RayCastInfo rayCast = LogicServer.physic2D().rayCastInfo(this, pos, targetPos);
        if (rayCast.hit && isFloorNormal(rayCast.normalDirection)) {
            isOnFloor = true;
            floorNormal.set(rayCast.normalDirection);
        }
    }

    private void floorSnap() {
        if (motionMode != MotionMode.Grounded) return;
        if (physicBodyRef == null || floorSnapDistance <= 0.0f) return;

        Vector2f currentPos = globalPosition();
        Vector2f snapDistance = new Vector2f(UpDirection).mul(-floorSnapDistance);
        Vector2f targetPos = new Vector2f(currentPos).add(snapDistance);

        RayCastInfo rayCast = LogicServer.physic2D().rayCastInfo(this, currentPos, targetPos);
        if (!rayCast.hit || !isFloorNormal(rayCast.normalDirection)) return;

        Vector2f snapMotion = new Vector2f(snapDistance).mul(rayCast.rayVectorFraction);
        Vector2f nextPos = new Vector2f(currentPos).add(snapMotion);
        physicBodyRef.setTransform(new Vec2(nextPos.x, nextPos.y), physicBodyRef.getAngle());

        isOnFloor = true;
        floorNormal.set(rayCast.normalDirection);
        velocity.y = 0.0f;
    }

    private static float angleInDegree(Vector2f normal, Vector2f upDirection) {
        return (float) Math.toDegrees(Math.acos(normal.dot(upDirection)));
    }

    private void resetMotionState() {
        isOnFloor = false;
        isOnWall = false;
        isOnCeiling = false;
        floorNormal.set(UpDirection);
        wallNormal.zero();
    }

    @Override
    public CharacterBody2D copy() {
        return copy(false);
    }

    @Override
    public CharacterBody2D copy(boolean copyHierarchy) {
        CharacterBody2D copy = (CharacterBody2D) copySingleObject();

        if (copyHierarchy && !getChildren().isEmpty()) copyDescendants(this, copy);

        return copy;
    }

    @Override
    public void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openChar = ImGui.collapsingHeader("CharacterBody2D##CharacterBody2D_Properties_Header_" + getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openChar) {
            super.additionalImGuiLogic();
            return;
        }

        ImGui.indent();
        ImGui.text("Motion Mode:");
        MotionMode currentMode = motionMode;
        if (ImGui.beginCombo("##Select_Character_Motion_Mode_Combo_" + getUUID(), currentMode.name())) {
            for (MotionMode mode : MotionMode.values()) {
                String display = mode.name();
                String label = display + "##Select_" + display + "_Selectable_" + getUUID();
                if (ImGui.selectable(label, mode == currentMode)) motionMode(mode);
            }
            ImGui.endCombo();
        }
        ImGui.spacing();
        Vector2f upDir = new Vector2f(UpDirection);
        if (EditorWidget.dragVec2Ctrl("Up Direction", upDir, 0.0f, 1.0f, 0.1f, this)) upDirection(upDir);

        if (motionMode == MotionMode.Grounded) {
            ImGui.spacing();
            float slopeAngle = EditorWidget.dragFloatCtrl("Max Slope Angle", maxSlopeAngle, 45.0f, 1.0f, this, 0.0f, 90.0f);
            if (Float.compare(slopeAngle, maxSlopeAngle) != 0) maxSlopeAngle(slopeAngle);
            ImBoolean floorSnap = new ImBoolean(snapToFloor);
            if (ImGui.checkbox("Snap to floor##CharacterBody2D_SnapToFloor_CheckBox_" + getUUID(), floorSnap)) snapToFloor = floorSnap.get();
            if (snapToFloor) {
                float snapDistance = EditorWidget.dragFloatCtrl("Floor Snapping Distance", floorSnapDistance, 0.1f, 0.1f, this, 0.0f);
                if (Float.compare(snapDistance, floorSnapDistance) != 0) floorSnapDistance(snapDistance);
            }
        }

        ImGui.spacing();
        float margin = EditorWidget.dragFloatCtrl("Safe Margin", safeMargin, 0.01f, this, 0.001f);
        if (Float.compare(margin, safeMargin) != 0) safeMargin(margin);

        ImGui.unindent();
        super.additionalImGuiLogic();
    }
}

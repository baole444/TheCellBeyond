package physic2d;

import TheCellBeyond.internal.LogicServer;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyDef;
import org.joml.Vector2f;
import physic2d.enums.MotionMode;
import physic2d.enums.PhysicBodyType;

/**
 * CharacterBody2D is a specialized physic body that are meant for user control.
 * They are not affected by physic, but will affect other physic body in its path.
 * <p>
 * The main purpose of this class is to provide API to move objects with wall and slope detection,
 * in additional to collision detection. This make it useful for physic bodies that must move
 * in specific ways and collide with the world, which is often the case for user-controlled characters.
 */
public class CharacterBody2D extends PhysicBody2D {
    /**
     * The limit on how many times can the character change direction per {@link #moveAndSlide()} call.
     */
    private static final int MaxCollisionSlide = 4;
    /**
     * The vector that define the upward direction, used for determine if the surface is floor,
     * wall or ceiling.
     */
    private final Vector2f upDirection = new Vector2f(0.0f, 1.0f);
    /**
     * The maximum angle in degrees for a surface to still be considered a floor.
     * The value is clamped to {@code [0, 90]} internally.
     */
    public float maxFloorAngle = 45.0f;
    /**
     * The snapping distance in world units, keeping the character attached to slopes when calling {@link #moveAndSlide()}.
     * The snapping vector is calculated using this distance and the opposite of {@link #upDirection()} vector.
     * <p>
     * When set to value other than {@code 0.0}, a ray is cast opposite to {@link #upDirection()} by this distance.
     * As long as this ray hit the ground, the character will remain attached to the surface.
     * Snapping is not applied when the character moves along the {@link #upDirection()} vector,
     * allow it to be detached from the ground (jumping for example.)
     */
    public float floorSnapDistance = 0.01f;
    /**
     * The motion mode of the character, which define the behaviour of {@link #moveAndSlide()}.
     */
    public MotionMode motionMode = MotionMode.Grounded;
    /**
     * The distance used to create buffer for collision recovery when calling {@link #moveAndSlide()}, in world units.
     * <p>
     * If the body is at least this close to another body, it will consider them to be colliding and will be pushed away
     * before performing actual motion.
     * </p>
     * A higher value mean it's more flexible for detecting collision,
     * which help with consistently detecting walls and floors.
     * <p>
     * A lower value force the collision algorithm to use more exact detection, so it can be used in case that specifically
     * require precision. For example, a very low scale to avoid visible jittering,
     * or for stability when stacking character bodies.
     */
    public float safeMargin = 0.01f;
    /**
     * The current velocity vector of the character in world units, used and modified by calls to {@link #moveAndSlide()}.
     * @apiNote
     * It is a common mistake to set the value of the desired velocity using a motion vector's value,
     * which is velocity multiplied by {@code delta}.
     */
    public transient final Vector2f velocity = new Vector2f();
    private transient PhysicBody2D platformBody = null;
    private transient boolean isOnFloor = false;
    private transient boolean isOnWall = false;
    private transient boolean isOnCeiling = false;
    private transient final Vector2f floorNormal = new Vector2f(0.0f, 1.0f);
    private transient final Vector2f wallNormal = new Vector2f();
    private transient int slideCollisionCount = 0;

    /**
     * Create a new {@link CharacterBody2D} object.
     */
    public CharacterBody2D() {
        String name = CharacterBody2D.class.getSimpleName();
        this(name);
    }

    /**
     * Create a new {@link CharacterBody2D} with the given name.
     * @param name the new name for the object
     */
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
    public void configurePhysicBodyRef() {}

    @Override
    protected void onPhysicUpdate(float dt) {
        if (physicBodyRef == null) return;
        resetMotionState();
    }

    @Override
    public Vector2f linearVelocity() {
        return new Vector2f(velocity);
    }

    /**
     * Set the movement velocity of this character, this allows control accumulation logic for the velocity elsewhere.
     * @param velocity the velocity vector to add (unit: m/s)
     */
    @Override
    public void addMovement(Vector2f velocity) {
        this.velocity.set(velocity);
    }

    /**
     * Reset the velocity for this character to zero, can be call at begin of frame to clear velocity.
     * If the character is controlled via {@link components.Controller2D},
     * calling this method can nullify the effect of the controller.
     */
    @Override
    public void resetMovement() {
        velocity.zero();
    }

    /**
     * Get the up direction of the character.
     * @return a copy of the up direction vector
     */
    public Vector2f upDirection() {
        return new Vector2f(upDirection);
    }

    /**
     * Set the up direction vector for the character.
     * <p>
     * This will set the up direction vector then normalize it.
     * @param upDirection the new up direction vector
     */
    public void upDirection(Vector2f upDirection) {
        if (upDirection == null) return;
        this.upDirection.set(upDirection).normalize();
    }

    /**
     * Check if the character collided with the floor since the last call of {@link #moveAndSlide()}.
     * <p>
     * A surface is considered a floor base on the {@link #upDirection()} vector and {@link #maxFloorAngle}.
     * Only characters in {@link MotionMode#Grounded} have floor collision.
     * @return true if collided with a floor
     */
    public boolean isOnFloor() {
        return motionMode == MotionMode.Grounded && isOnFloor;
    }

    /**
     * Check if the character only collided with the floor since the last call of {@link #moveAndSlide()}.
     * <p>
     * A surface is considered a floor base on the {@link #upDirection()} vector and {@link #maxFloorAngle}.
     * Only characters in {@link MotionMode#Grounded} have floor collision.
     * @return true if collided with a floor, but not wall or ceiling
     */
    public boolean isOnFloorOnly() {
        return motionMode == MotionMode.Grounded && isOnFloor && !isOnWall && !isOnCeiling;
    }

    /**
     * Check if the character collided with the wall since the last call of {@link #moveAndSlide()}.
     * <p>
     * A surface is considered a wall base on the {@link #upDirection()} vector and {@link #maxFloorAngle}.
     * Characters in {@link MotionMode#Floating} have all their collision classified as wall collision.
     * @return true if collided with a wall
     */
    public boolean isOnWall() {
        return isOnWall;
    }

    /**
     * Check if the character only collided with the wall since the last call of {@link #moveAndSlide()}.
     * <p>
     * A surface is considered a wall base on the {@link #upDirection()} vector and {@link #maxFloorAngle}.
     * Characters in {@link MotionMode#Floating} have all their collision classified as wall collision.
     * @return true if collided with a wall, but not floor or ceiling
     */
    public boolean isOnWallOnly() {
        return isOnWall && !isOnFloor && !isOnCeiling;
    }

    /**
     * Check if the character collided with the ceiling since the last call of {@link #moveAndSlide()}.
     * <p>
     * A surface is considered a ceiling base on the {@link #upDirection()} vector and {@link #maxFloorAngle}.
     * Characters in {@link MotionMode#Floating} have all their collision classified as wall collision.
     * @return true if collided with a ceiling
     */
    public boolean isOnCeiling() {
        return motionMode == MotionMode.Grounded && isOnCeiling;
    }

    /**
     * Check if the character is only on ceiling.
     * @return true if the character is currently only on ceiling
     */
    public boolean isOnCeilingOnly() {
        return motionMode == MotionMode.Grounded && isOnCeiling && !isOnFloor && !isOnWall;
    }

    /**
     * Check if the character had collided with wall, floor or ceiling.
     * @return true if one of the 3 surface had collided
     */
    public boolean isCollided() {
        return isOnFloor || isOnWall || isOnCeiling;
    }

    /**
     * Get the collision normal of the floor at last collision point.
     * <p>
     * The value returned is only valid after calling {@link #moveAndSlide()} and {@link #isOnFloor()} return true.
     * @return a copy of the floor normal vector
     */
    public Vector2f floorNormal() {
        return new Vector2f(floorNormal);
    }

    /**
     * Get the collision normal of the wall and the last collision point.
     * <p>
     * The value returned is only valid after calling {@link #moveAndSlide()} and {@link #isOnWall()} return true.
     * @return a copy of the wall normal vector
     */
    public Vector2f wallNormal() {
        return new Vector2f(wallNormal);
    }

    /**
     * Get the collision normal of at the last collision point.
     * <p>
     * The method prioritize floor -> wall -> ceiling normal as return order.
     * If no collision happened, a zero vector is return.
     * @return a copy of one of the collision normal vector
     */
    public Vector2f getCollisionNormal() {
        if (isOnFloor) return floorNormal();
        if (isOnWall) return  wallNormal();
        if (isOnCeiling) return new Vector2f(upDirection).negate();
        return new Vector2f();
    }

    /**
     * Move the body based on {@link #velocity}. If the body collides with another, it will slide along the other body
     * (by default only on floor) rather than stop immediately. If the other body is a {@link CharacterBody2D} or {@link RigidBody2D},
     * it will also be affected by the motion of other body. This can be used to make moving and rotating platforms,
     * or to make objects push other objects.
     * <p>
     * This method should be use in {@link #onPhysicUpdate(float)} or {@link #physicUpdate(float)},
     * as it uses the physic step's delta value automatically in calculations.
     * Otherwise, the simulation will run at an incorrect speed.
     * </p>
     * When the body touches a moving platform, the platform's velocity is automatically added to the body's motion.
     * <p>
     * The general behaviour and available properties change according to the {@link #motionMode}.
     * @return true if the body collided
     */
    public boolean moveAndSlide() {
        if (physicBodyRef == null) return false;
        PhysicBody2D lastPlatform = platformBody;
        platformBody = null;
        Vector2f motion = new Vector2f(velocity);
        if (lastPlatform != null) motion.add(lastPlatform.linearVelocity());
        motion.mul(Physic2D.PhysicDeltaRate);
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
            if (!isOnFloor && velocity.dot(upDirection) <= 0.0f) floorSnap();
        }
        return collided;
    }

    private Vector2f slideMotion(Vector2f motion) {
        Physic2D physic2D = LogicServer.currentScenePhysic2D();
        if (physic2D == null) return new Vector2f();
        float margin = Math.max(0.001f, safeMargin);
        if (slideCollisionCount >= MaxCollisionSlide || motion.lengthSquared() < margin * margin) return new Vector2f();
        slideCollisionCount++;
        Vector2f currentPos = globalPosition();
        Vector2f targetPos = new Vector2f(currentPos).add(motion);
        RayCastInfo rayCast = physic2D.rayCastInfo(this, currentPos, targetPos);
        if (!rayCast.hit || rayCast.contactedObject == null) return motion;
        if (rayCast.contactedObject instanceof PhysicBody2D physicBody) platformBody = physicBody;
        float fraction = rayCast.rayVectorFraction;
        Vector2f hitNormal = rayCast.normalDirection;
        Vector2f safeMotion = new Vector2f(motion).mul(Math.max(0.0f, fraction - margin));
        Vector2f remainMotion = new Vector2f(motion).sub(safeMotion);
        float dot = remainMotion.dot(hitNormal);
        Vector2f slideVector = new Vector2f(remainMotion).sub(new Vector2f(hitNormal).mul(dot));
        handleCollision(hitNormal, slideVector);
        Vector2f nextSlide = slideMotion(slideVector);
        return new Vector2f(safeMotion).add(nextSlide);
    }

    private void handleCollision(Vector2f normal, Vector2f slideVelocity) {
        if (motionMode != MotionMode.Grounded) {
            isOnWall = true;
            wallNormal.set(normal);
            return;
        }
        categorizeCollision(normal);
        float upComponent = velocity.dot(upDirection);
        if ((isFloorNormal(normal) && upComponent <= 0.0f) || (isCeilingNormal(normal) && upComponent >= 0.0f)) {
            velocity.sub(upDirection.x * upComponent, upDirection.y * upComponent);
            float slideUpComponent = slideVelocity.dot(upDirection);
            slideVelocity.sub(upDirection.x * slideUpComponent, upDirection.y * slideUpComponent);
        }
    }

    private void categorizeCollision(Vector2f normal) {
        if (motionMode != MotionMode.Grounded) {
            isOnWall = true;
            wallNormal.set(normal);
            return;
        }
        float clampedAngle = Math.clamp(maxFloorAngle, 0.0f, 90.0f);
        float angle = angleInDegree(normal, upDirection);
        if (angle <= clampedAngle) {
            isOnFloor = true;
            floorNormal.set(normal);
            return;
        }
        if (angle >= 180.0f - clampedAngle) {
            isOnCeiling = true;
            return;
        }
        isOnWall = true;
        wallNormal.set(normal);
    }

    private boolean isFloorNormal(Vector2f normal) {
        if (motionMode != MotionMode.Grounded) return false;
        float angle = angleInDegree(normal, upDirection);
        return angle <= Math.clamp(maxFloorAngle, 0.0f, 90.0f);
    }

    private boolean isCeilingNormal(Vector2f normal) {
        if (motionMode != MotionMode.Grounded) return false;
        float angle = angleInDegree(normal, upDirection);
        return angle >= 180.0f - Math.clamp(maxFloorAngle, 0.0f, 90.0f);
    }

    private void checkFloorState() {
        Physic2D physic2D = LogicServer.currentScenePhysic2D();
        if (physic2D == null) return;
        if (motionMode != MotionMode.Grounded) return;
        if (physicBodyRef == null) return;
        Vector2f pos = globalPosition();
        float margin = Math.max(0.001f, safeMargin);
        Vector2f safeDistance = new Vector2f(upDirection).mul(-margin * 2.0f);
        Vector2f targetPos = new Vector2f(pos).add(safeDistance);
        RayCastInfo rayCast = physic2D.rayCastInfo(this, pos, targetPos);
        if (rayCast.hit && isFloorNormal(rayCast.normalDirection)) {
            isOnFloor = true;
            floorNormal.set(rayCast.normalDirection);
        }
    }

    private void floorSnap() {
        Physic2D physic2D = LogicServer.currentScenePhysic2D();
        if (physic2D == null) return;
        if (motionMode != MotionMode.Grounded) return;
        if (physicBodyRef == null || floorSnapDistance == 0.0f) return;
        Vector2f currentPos = globalPosition();
        Vector2f snapDistance = new Vector2f(upDirection).mul(-floorSnapDistance);
        Vector2f targetPos = new Vector2f(currentPos).add(snapDistance);
        RayCastInfo rayCast = physic2D.rayCastInfo(this, currentPos, targetPos);
        if (!rayCast.hit || !isFloorNormal(rayCast.normalDirection)) return;
        Vector2f snapMotion = new Vector2f(snapDistance).mul(rayCast.rayVectorFraction);
        Vector2f nextPos = new Vector2f(currentPos).add(snapMotion);
        physicBodyRef.setTransform(new Vec2(nextPos.x, nextPos.y), physicBodyRef.getAngle());
        isOnFloor = true;
        floorNormal.set(rayCast.normalDirection);
        float upComponent = velocity.dot(upDirection);
        velocity.sub(upDirection.x * upComponent, upDirection.y * upComponent);
    }

    private static float angleInDegree(Vector2f normal, Vector2f upDirection) {
        return (float) Math.toDegrees(Math.acos(normal.dot(upDirection)));
    }

    private void resetMotionState() {
        isOnFloor = false;
        isOnWall = false;
        isOnCeiling = false;
        floorNormal.set(upDirection);
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
                if (ImGui.selectable(label, mode == currentMode)) motionMode = mode;
            }
            ImGui.endCombo();
        }
        ImGui.spacing();
        Vector2f upDir = new Vector2f(upDirection);
        if (EditorWidget.dragVec2Ctrl("Up Direction", upDir, 0.0f, 1.0f, 0.1f, this)) upDirection(upDir);
        if (motionMode == MotionMode.Grounded) {
            ImGui.spacing();
            float slopeAngle = EditorWidget.dragFloatCtrl("Max Slope Angle", maxFloorAngle, 45.0f, 1.0f, this, 0.0f, 90.0f);
            if (Float.compare(slopeAngle, maxFloorAngle) != 0) maxFloorAngle = slopeAngle;
            float snapDistance = EditorWidget.dragFloatCtrl("Floor Snapping Distance", floorSnapDistance, 0.1f, 0.1f, this);
            if (Float.compare(snapDistance, floorSnapDistance) != 0) floorSnapDistance = snapDistance;
        }
        ImGui.spacing();
        float margin = EditorWidget.dragFloatCtrl("Safe Margin", safeMargin, 0.01f, this, 0.001f);
        if (Float.compare(margin, safeMargin) != 0) safeMargin = margin;
        ImGui.unindent();
        super.additionalImGuiLogic();
    }
}

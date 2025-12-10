package physic2d;

import org.jbox2d.dynamics.BodyDef;
import org.joml.Vector2f;
import physic2d.enums.MotionMode;
import physic2d.enums.PhysicBodyType;

public class CharacterBody2D extends PhysicBody2D {
    private static final int MaxSlide = 4;
    private static final Vector2f UpDirection = new Vector2f(0.0f, 1.0f);

    private float maxSlopeAngle = 45.0f;
    private float floorSnapLength = 0.1f;
    private boolean snapToFloor = true;

    private MotionMode motionMode = MotionMode.Grounded;
    private float safeMargin = 0.08f;

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
    protected void additionalPhysicUpdate(float dt) {
        if (physicBodyRef == null) return;
        resetMotionState();
    }

    private void resetMotionState() {
        isOnFloor = false;
        isOnWall = false;
        isOnCeiling = false;
        floorNormal.set(UpDirection);
        wallNormal.zero();
    }

    public boolean moveAndSlide() {
        return false;
    }
}

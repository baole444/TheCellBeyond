package components;

import TheCellBeyond.KeyListener;
import TheCellBeyond.Window;
import org.joml.Math;
import org.joml.Vector2f;
import physic2d.components.PhysicBody2D;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;

// TODO: Update this once other system finalized.
//  Temporary ignore this.
public class CharacterControl extends Component {
    private final float walkSpeed = 1f;

    private final float jumpBoost = 1f;

    private final float jumpImpulse = 3.0f;

    private final float slowDownForce = 1f;

    private final transient Vector2f velocity = new Vector2f();

    private final transient Vector2f acceleration = new Vector2f();

    private final Vector2f terminalVelocity = new Vector2f(1f, 1f);

    // These are ground check, useful in platforming game.
    private final transient boolean isOnGround = false;

    private final transient float groundInit = 0.0f;

    /**
     * Limit how much time a user can initialize a jump
     * on the edge of a collider surface.
     */
    private final transient float groundDebounceTIme = 0.0f;

    private transient PhysicBody2D physicBody2D;

    private transient StateEngine stateEngine;

    private final transient float charWidth = 0.32f;
    private final transient float charHeight = 0.32f;

    /**
     * Store how long the user hold down the jump key.
     */
    private final transient int jumpTime = 0;

    private final transient boolean isDead = false;

    @Override
    public void start() {
        this.physicBody2D = gameObject.getFirstComponent(PhysicBody2D.class);
        this.stateEngine = gameObject.getFirstComponent(StateEngine.class);

        // Set gravity to 0 to manage custom physic
        this.physicBody2D.setGravityScale(0.0f);
    }

    @Override
    public void update(float dt) {
        if (KeyListener.isKeyPressed(GLFW_KEY_RIGHT)) {
            //this.gameObject.transform.scale.x = charWidth;
            this.acceleration.x = walkSpeed;

            if (this.velocity.x < 0) {
                //this.stateEngine.condition("switch");
                this.velocity.x += slowDownForce;
            } else {
                //this.stateEngine.condition("run");
            }
        } else if (KeyListener.isKeyPressed(GLFW_KEY_LEFT)) {
            //this.gameObject.transform.scale.x = -charWidth;
            this.acceleration.x = -walkSpeed;

            if (this.velocity.x > 0) {
                //this.stateEngine.condition("switch");
                this.velocity.x -= slowDownForce;
            } else {
                //this.stateEngine.condition("run");
            }
        } else {
            this.acceleration.x = 0;

            if (this.velocity.x > 0) {
                this.velocity.x = Math.max(0, this.velocity.x - slowDownForce);
            } else if (this.velocity.x < 0) {
                this.velocity.x = Math.min(0, this.velocity.x + slowDownForce);
            }

            if (this.velocity.x == 0) {
                //this.stateEngine.condition("stop");
            }
        }

        this.acceleration.y = Window.getPhysic2D().getGravity().y * 0.7f;

        this.velocity.x += this.acceleration.x * dt;
        this.velocity.y += this.acceleration.y * dt;

        // Limit speed to terminal velocity
        this.velocity.x = Math.max(
                Math.min(this.velocity.x, this.terminalVelocity.x),
                -this.terminalVelocity.x
        );

        this.velocity.y = Math.max(
                Math.min(this.velocity.y, this.terminalVelocity.y),
                -this.terminalVelocity.y
        );

        this.physicBody2D.setVelocity(this.velocity);
        this.physicBody2D.setAngularVelocity(0.0f);
    }
}
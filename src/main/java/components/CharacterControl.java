package components;

import TCB_Field.KeyListener;
import TCB_Field.Window;
import org.joml.Math;
import org.joml.Vector2f;
import physic_2d.components.FlatPhysicBody;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;

public class CharacterControl extends Component {
    private float walkSpeed = 1f;

    private float jumpBoost = 1f;

    private float jumpImpulse = 3.0f;

    private float slowDownForce = 1f;

    private transient Vector2f velocity = new Vector2f();

    private transient Vector2f acceleration = new Vector2f();

    private Vector2f terminalVelocity = new Vector2f(1f, 1f);

    // These are ground check, useful in platforming game.
    private transient boolean isOnGround = false;

    private transient float groundInit = 0.0f;

    /**
     * Limit how much time a user can initialize a jump
     * on the edge of a collider surface.
     */
    private transient float groundDebounceTIme = 0.0f;

    private transient FlatPhysicBody flatPhysicBody;

    private transient StateEngine stateEngine;

    private transient float charWidth = 0.32f;
    private transient float charHeight = 0.32f;

    /**
     * Store how long the user hold down the jump key.
     */
    private transient int jumpTime = 0;

    private transient boolean isDead = false;

    @Override
    public void start() {
        this.flatPhysicBody = gameObject.getComponent(FlatPhysicBody.class);
        this.stateEngine = gameObject.getComponent(StateEngine.class);

        // Set gravity to 0 to manage custom physic
        this.flatPhysicBody.setGravityScale(0.0f);
    }

    @Override
    public void update(float dt) {
        if (KeyListener.isKeyPressed(GLFW_KEY_RIGHT)) {
            this.gameObject.transform.scale.x = charWidth;
            this.acceleration.x = walkSpeed;

            if (this.velocity.x < 0) {
                //this.stateEngine.condition("switch");
                this.velocity.x += slowDownForce;
            } else {
                //this.stateEngine.condition("run");
            }
        } else if (KeyListener.isKeyPressed(GLFW_KEY_LEFT)) {
            this.gameObject.transform.scale.x = -charWidth;
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

        this.acceleration.y = Window.getFlatPhysic().loadGravity().y * 0.7f;

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

        this.flatPhysicBody.setVelocity(this.velocity);
        this.flatPhysicBody.setAngularVelocity(0.0f);
    }





    

}

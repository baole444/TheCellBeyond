package threading.states;

import TheCellBeyond.GameObject;
import org.joml.Vector2f;
import physic2d.Physic2D;
import physic2d.components.PhysicBody2D;

import java.util.HashMap;
import java.util.Map;

public class PhysicWorldState {
    private Vector2f gravity;
    private Map<Integer, PhysicsBodyState> physicBodies;

    public PhysicWorldState(Physic2D physic) {
        this.gravity = new Vector2f(physic.getGravity());
        this.physicBodies = new HashMap<>();

        // TODO: Access to scene's game objects and
        //  iterate through all objects with physic bodies.
    }

    public void capturePhysicsState(GameObject gameObject) {
        PhysicBody2D body = gameObject.getComponent(PhysicBody2D.class);
        if (body != null && body.getInstObjectBody() != null) {
            PhysicsBodyState state = new PhysicsBodyState(
                    gameObject.getUID(),
                    new Vector2f(
                            body.getInstObjectBody().getPosition().x,
                            body.getInstObjectBody().getPosition().y
                    ),
                    new Vector2f(body.getVelocity()),
                    body.getAngularVelocity(),
                    body.getInstObjectBody().getAngle()
            );

            physicBodies.put(state.getObjectId(), state);
        }
    }

    public Vector2f getGravity() {
        return gravity;
    }

    public Map<Integer, PhysicsBodyState> getPhysicBodies() {
        return physicBodies;
    }
}

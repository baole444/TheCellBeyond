package components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.InputAction;
import physic2d.PhysicBody2D;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class Controller2D extends Component {
    /**
     * How this controller apply
     */
    public enum ControlMode {
        SpatialLogic,
        Physical
    }

    public float movementSpeed = 0.0f;
    private final ConcurrentHashMap<ControllerDirection, HashSet<InputAction>> directionActionBindings = new ConcurrentHashMap<>();
    private ControlMode controlMode = ControlMode.Physical;

    private transient GameObject2D gameObject2D = null;
    private transient PhysicBody2D physicBody2D = null;
    private transient boolean isGameObjectSpatialCompatible = false;
    private transient boolean isGameObjectPhysicCompatible = false;


    @Override
    protected void additionalStartLogic() {
        if (gameObject == null) return;
        if (gameObject instanceof GameObject2D go2D) {
            gameObject2D = go2D;
            isGameObjectSpatialCompatible = true;
        }
        if (gameObject instanceof PhysicBody2D body2D) {
            physicBody2D = body2D;
            isGameObjectPhysicCompatible = true;
        }

        if (!isGameObjectPhysicCompatible) controlMode = ControlMode.SpatialLogic;
    }

    @Override
    protected void additionalDestroyLogic() {
        gameObject2D = null;
        physicBody2D = null;
    }

    @Override
    protected void additionalUpdateLogic(float dt) {

    }
}

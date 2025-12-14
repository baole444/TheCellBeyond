package components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.Input;
import TheCellBeyond.InputAction;
import org.joml.Vector2f;
import physic2d.PhysicBody2D;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller2D component allows applying movement velocity to the game object it is mounted to (if supported).
 * <p>
 * The movement is applied base on {@link ControllerDirection}s and their bound {@link InputAction}s.
 * @see ControlMode Controller2D's control modes
 */
public class Controller2D extends Component {

    /**
     * ControlMode define how the controller apply movement.
     * <p>
     * There are 2 modes:
     * <ul>
     *     <li>{@link ControlMode#SpatialLogic}</li>
     *     <li>{@link ControlMode#PhysicalLogic}</li>
     * </ul>
     */
    public enum ControlMode {

        /**
         * The game object cannot be moved
         */
        Incompatible,

        /**
         * Move the game object using spatial transformation logic.
         * Movement velocity is scaled by delta time.
         * <p>
         * Require the game object of the controller to be of type {@link GameObject2D} or its subclasses.
         */
        SpatialLogic,

        /**
         * Move the game object using physics logic.
         * Movement velocity is added to the physic body without delta time scaling.
         * <p>
         * Require the game object of the controller to be of type {@link PhysicBody2D} or its subclasses.
         */
        PhysicalLogic
    }

    /**
     * Movement speed to apply to the direction vector, in world unit.
     */
    public float movementSpeed = 0.0f;

    /**
     * Is the controller only accept the first active action in a frame.
     */
    public boolean oneActionPerFrame = true;

    /**
     * Is the diagonal movement speed applied by this controller normalized.
     */
    public boolean normalizeDiagonalSpeed = true;

    private final ConcurrentHashMap<ControllerBinding, HashSet<InputAction>> controllerActionBindings = new ConcurrentHashMap<>();

    /**
     * The control mode of this controller.
     */
    private ControlMode controlMode = ControlMode.PhysicalLogic;

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

        if (!isGameObjectSpatialCompatible && !isGameObjectPhysicCompatible) {
            controlMode = ControlMode.Incompatible;
            LOGGER.warning(String.format("Mounting %s to unsupported '%s' object", this.getClass().getSimpleName(), gameObject.name));
            return;
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
        if (controlMode == ControlMode.Incompatible) return;

        Vector2f finalDirection = getCombinedDirection(dt);
        if (finalDirection.lengthSquared() == 0.0f) return;
        if (normalizeDiagonalSpeed) finalDirection.normalize();
        finalDirection.mul(movementSpeed);

        if (controlMode == ControlMode.PhysicalLogic) {
            applyPhysicalMovement(finalDirection);
            return;
        }

        if (controlMode == ControlMode.SpatialLogic) applySpatialMovement(finalDirection, dt);
    }

    public ControlMode controlMode() {
        return controlMode;
    }

    public void controlMode(ControlMode mode) {
        if (mode == null || mode == ControlMode.Incompatible) return;
        if (!isGameObjectPhysicCompatible && !isGameObjectSpatialCompatible) {
            controlMode = ControlMode.Incompatible;
            return;
        }

        if (!isGameObjectSpatialCompatible) return;

        if (!isGameObjectPhysicCompatible) {
            controlMode = ControlMode.SpatialLogic;
            return;
        }

        controlMode = mode;
    }

    public void bindAction(String bindingName, String actionName) {
        if (bindingName == null || actionName == null) return;

        InputAction inputAction = Input.getInputAction(actionName);
        if (inputAction == null) return;
        ControllerBinding binding = null;
        for (Map.Entry<ControllerBinding, HashSet<InputAction>> entry : controllerActionBindings.entrySet()) {
            ControllerBinding controllerBinding = entry.getKey();
            if (!controllerBinding.name.equals(bindingName)) continue;
            binding = controllerBinding;
        }

        if (binding == null) return;
        bindAction(binding, inputAction);
    }

    public void bindAction(ControllerBinding binding, InputAction action) {
        if (binding == null || action == null) return;
        controllerActionBindings.computeIfAbsent(binding, k -> new HashSet<>()).add(action);
    }

    public void unbindAction(String bindingName, String actionName) {
        if (bindingName == null || actionName == null) return;

        InputAction inputAction = Input.getInputAction(actionName);
        if (inputAction == null) return;
        ControllerBinding binding = null;
        for (Map.Entry<ControllerBinding, HashSet<InputAction>> entry : controllerActionBindings.entrySet()) {
            ControllerBinding controllerBinding = entry.getKey();
            if (!controllerBinding.name.equals(bindingName)) continue;
            binding = controllerBinding;
        }

        if (binding == null) return;
        unbindAction(binding, inputAction);
    }

    public void unbindAction(ControllerBinding binding, InputAction action) {
        if (binding == null || action == null) return;

        HashSet<InputAction> actions = controllerActionBindings.get(binding);
        if (actions == null || actions.isEmpty()) return;
        actions.remove(action);
    }

    private Vector2f getCombinedDirection(float dt) {
        Vector2f combinedDirection = new Vector2f();
        if (controllerActionBindings.isEmpty()) return combinedDirection;
        for (Map.Entry<ControllerBinding, HashSet<InputAction>> entry : controllerActionBindings.entrySet()) {
            ControllerBinding controllerBinding = entry.getKey();
            HashSet<InputAction> actions = entry.getValue();

            boolean isPressed = false;
            for (InputAction action : actions) {
                if (!Input.isActionPresses(action.name())) continue;
                isPressed = true;
                break;
            }

            if (!controllerBinding.isActive(isPressed, dt)) continue;
            combinedDirection.add(controllerBinding.directionVector());

            if (oneActionPerFrame) break;
        }
        return combinedDirection;
    }

    /**
     * Apply movement by update this component's {@link GameObject2D}'s position.
     * The movement is scaled by delta time when moving this way.
     * @param movementVelocity the movement velocity of movement direction and speed
     * @param dt the delta time of this frame
     */
    private void applySpatialMovement(Vector2f movementVelocity, float dt) {
        if (!isGameObjectSpatialCompatible || gameObject2D == null) return;

        Vector2f displacement = new Vector2f(movementVelocity).mul(dt);
        Vector2f currentPos = gameObject2D.getPosition();
        gameObject2D.setPosition(currentPos.x + displacement.x, currentPos.y + displacement.y);
    }

    /**
     * Apply movement by adding velocity to this component's {@link PhysicBody2D}.
     * The movement is not scaled by delta time when moving this way.
     * @param movementVelocity the movement velocity of movement direction and speed
     */
    private void applyPhysicalMovement(Vector2f movementVelocity) {
        if (!isGameObjectPhysicCompatible || physicBody2D == null) return;
        physicBody2D.addMovement(movementVelocity);
    }
}


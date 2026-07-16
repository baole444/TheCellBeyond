package components;

import org.joml.Vector2f;
import scripting.API;

import java.util.HashSet;

/**
 * ControllerBinding contains the direction, activation mechanic, and triggering input actions for a binding of a {@link Controller2D}.
 */
@API
public class ControllerBinding {
    /**
     * The direction of this controller binding.
     */
    public final ControllerDirection direction;
    /**
     * The activation condition of this controller binding.
     */
    public final InputActivation activation;
    /**
     * The set of name of input actions that activate this controller binding.
     */
    public final HashSet<String> boundActionNames = new HashSet<>();

    /**
     * Create a new {@link ControllerBinding} with no direction and default activation condition.
     */
    public ControllerBinding() {
        direction = new ControllerDirection();
        activation = new InputActivation();
    }

    /**
     * Create a new {@link ControllerBinding} with the given direction and default activation condition.
     * @param direction the direction for the new binding
     */
    public ControllerBinding(ControllerDirection direction) {
        this.direction = new ControllerDirection(direction);
        activation = new InputActivation();
    }

    /**
     * Create a new {@link ControllerBinding} with the given direction and activation condition.
     * @param direction the direction for the new binding
     * @param activation the condition for the new binding
     */
    public ControllerBinding(ControllerDirection direction, InputActivation activation) {
        this.direction = new ControllerDirection(direction);
        this.activation = new InputActivation(activation);
    }

    /**
     * Create a new {@link ControllerBinding} from the given controller binding.
     * @param binding the controller binding to create a new binding from
     */
    public ControllerBinding(ControllerBinding binding) {
        direction = new ControllerDirection(binding.direction);
        activation = new InputActivation(binding.activation);
        boundActionNames.addAll(binding.boundActionNames);
    }

    /**
     * Check if this controller binding is active or not.
     * @param isPressed input action press status
     * @param dt delta time
     * @return true if is active
     */
    public boolean isActive(boolean isPressed, float dt) {
        return activation.isActivated(isPressed, dt);
    }

    /**
     * Get the direction vector of this controller binding.
     * @return the direction vector
     */
    public Vector2f directionVector() {
        return direction.directionVector();
    }

    /**
     * Get the input activation mode of this controller binding.
     * @return the current activation mode
     */
    public InputActivation.ActivationMode activationMode() {
        return activation.activationMode();
    }

    /**
     * Reset the active state of this controller binding.
     */
    public void reset() {
        activation.reset();
    }

    /**
     * Get the activation progress of this controller binding (0.0 -> 1.0).
     * @param isPressed the action press status
     * @return the activation progress value
     */
    public float activationProgress(boolean isPressed) {
        return activation.activationProgress(isPressed);
    }
}

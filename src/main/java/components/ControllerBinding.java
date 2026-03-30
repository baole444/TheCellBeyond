package components;

import org.joml.Vector2f;

import java.util.HashSet;

/**
 * ControllerBinding contains the direction, activation mechanic, and triggering input actions for a binding of a {@link Controller2D}.
 */
public class ControllerBinding {
    public final ControllerDirection direction;
    public final InputActivation activation;
    public final HashSet<String> boundActionNames = new HashSet<>();

    public ControllerBinding() {
        direction = new ControllerDirection();
        activation = new InputActivation();
    }

    public ControllerBinding(ControllerDirection direction) {
        this.direction = direction;
        activation = new InputActivation();
    }

    public ControllerBinding(ControllerDirection direction, InputActivation activation) {
        this.direction = direction;
        this.activation = activation;
    }

    public ControllerBinding(ControllerBinding binding) {
        direction = new ControllerDirection(binding.direction);
        activation = binding.activation;
        boundActionNames.addAll(binding.boundActionNames);
    }

    public boolean isActive(boolean isPressed, float dt) {
        return activation.isActivated(isPressed, dt);
    }

    public Vector2f directionVector() {
        return direction.directionVector();
    }

    public InputActivation.ActivationMode activationMode() {
        return activation.activationMode();
    }

    public void reset() {
        activation.reset();
    }

    public float activationProgress(boolean isPressed) {
        return activation.activationProgress(isPressed);
    }
}

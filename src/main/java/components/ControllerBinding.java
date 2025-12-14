package components;

import org.joml.Vector2f;

public class ControllerBinding {
    public String name;
    public final ControllerDirection direction;
    public final InputActivation activation;

    public ControllerBinding(String name) {
        this.name = name;
        direction = new ControllerDirection();
        activation = new InputActivation();
    }

    public ControllerBinding(String name, ControllerDirection direction) {
        this.name = name;
        this.direction = direction;
        activation = new InputActivation();
    }

    public ControllerBinding(String name, ControllerDirection direction, InputActivation activation) {
        this.name = name;
        this.direction = direction;
        this.activation = activation;
    }

    public boolean isActive(boolean isPressed, float dt) {
        return activation.isActivated(isPressed, dt);
    }

    public Vector2f directionVector() {
        return direction.directionVector;
    }

    public void reset() {
        activation.reset();
    }

    public float activationProgress(boolean isPressed) {
        return activation.activationProgress(isPressed);
    }
}

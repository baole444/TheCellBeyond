package components;

import TheCellBeyond.Transform;
import org.joml.Vector2f;

public interface Transformation {

    /**
     * Get component's local transform
     */
    Transform getLocalTransform();

    /**
     * Get component's final transform (local transform with the Object's transform)
     */
    Transform getEffectiveTransform();

    /**
     * Set the component's local transform
     * @param transform the transform that will be used as offset for this component
     */
    void setLocalTransform(Transform transform);

    /**
     * Get the world position of the game object that own this component
     */
    Vector2f getObjectWorldPosition();
}

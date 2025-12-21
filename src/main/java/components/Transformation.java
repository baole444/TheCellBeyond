package components;

import TheCellBeyond.Transform2D;
import org.joml.Vector2f;

public interface Transformation {

    /**
     * Get the local transform of this transformation interface.
     * @return the local transform
     */
    Transform2D getLocalTransform();

    /**
     * Get the global transform of this transformation interface.
     * @return the global transform
     */
    Transform2D getEffectiveTransform();

    /**
     * Set the values for the local transform of this transformation interface.
     * @param transform2D the transform to update with
     */
    void setLocalTransform(Transform2D transform2D);

    /**
     * Get the global position from the owning 2D object of this interface.
     * @return the global positon vector
     */
    Vector2f getObjectWorldPosition();
}

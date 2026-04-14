package project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joml.Vector4f;

/**
 * ClearColor store the 4 colour components for the clear texture of window.
 * <p>
 * All component's values are clamped between 0 and 1.
 * @param r red component value
 * @param g green component value
 * @param b blue component value
 * @param a alpha component value
 */
public record ClearColor(float r, float g, float b, float a) {
    /**
     * Compact constructor, ensure colour components are within range.
     * @param r red component value
     * @param g green component value
     * @param b blue component value
     * @param a alpha component value
     */
    public ClearColor {
        r = Math.clamp(r, 0.0f, 1.0f);
        g = Math.clamp(g, 0.0f, 1.0f);
        b = Math.clamp(b, 0.0f, 1.0f);
        a = Math.clamp(a, 0.0f, 1.0f);
    }

    /**
     * Create the default clear colour.
     */
    @JsonIgnore
    public ClearColor() {
        this(0.027f, 0.122f, 0.067f, 1.0f);
    }

    /**
     * Create a clear colour from 4 components of the vector.
     * @param color the vector 4 to create clear colour from.
     */
    @JsonIgnore
    public ClearColor(Vector4f color) {
        this(color.x, color.y, color.z, color.w);
    }

    /**
     * Create a {@link Vector4f} from the 4 components of this clear colour.
     * @return a colour vector 4
     */
    @JsonIgnore
    public Vector4f toVector() {
        return new Vector4f(r, g, b, a);
    }
}

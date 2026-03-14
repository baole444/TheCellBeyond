package TheCellBeyond;

import components.Component;
import editor.template.EditorTemplate;
import org.joml.Vector2f;
import utility.WorldUnit;

/**
 * Transform2D holds position, scale, and rotation.
 * It represents the transformation in the logic spatial world (game world).
 * <p>
 * Transform2D also holds z-index, which dictates the rendering order for overlapping items.
 * This value only affects rendering elements of objects and components.
 * </p>
 * The Engine uses standard 2D coordinate system:
 * <ul>
 *     <li> Origin: bottom-left corner</li>
 *     <li> X-axis: increase to the right</li>
 *     <li> Y-axis: increase upward</li>
 *     <li> Rotation: positive angle rotate clockwise as viewed on screen
 *     (0 = right, 90 = down, 180 = left, 270 = up)
 *     </li>
 * </ul>
 * The unit for transform position is in {@link WorldUnit} and the rotation angle is in Degrees.
 * @see WorldUnit
 * @see <a href="https://en.wikipedia.org/wiki/Cartesian_coordinate_system#Two_dimensions">Cartesian coordinate system</a>
 * @see <a href="https://en.wikipedia.org/wiki/2D_computer_graphics#In_two_dimensions">Rotation in 2D</a>
 * @apiNote
 * Rotation is Y-flip, which make positive rotation angle appear clockwise. It is still counter-clockwise rotation mathematically.
 */
public class Transform2D extends Component {
    /**
     * The transform position vector.
     * Default value: (0.0, 0.0).
     */
    public final Vector2f position = new Vector2f();

    /**
     * The transform scale vector.
     * Default value: (1.0, 1.0).
     */
    public final Vector2f scale = new Vector2f(1.0f);

    /**
     * The transform rotation angle.
     */
    public float rotation = 0.0f;

    /**
     * The transform z-index value.
     */
    public int zIndex = 0;

    /**
     * Is this transform's z-index value relative or not.
     * <p>
     * Relative z-index mean the value can be combined between 2D objects in hierarchy
     * or between 2D object and spatial component.
     * </p>
     * Absolute z-index mean the value is final in world space.
     */
    public boolean relativeZIndex = true;

    /**
     * Create a new {@link Transform2D} and initialize its components to the default values.
     */
    public Transform2D() {
        String name = Transform2D.class.getSimpleName();
        this(name);
    }

    public Transform2D(String name) {
        super(name);
    }

    /**
     * Create a new {@link Transform2D} and initialize its components with the given position.
     * @param position the position vector to initialize with
     */
    public Transform2D(Vector2f position) {
        this.position.set(position);
    }

    /**
     * Create a new {@link Transform2D} and initialize its components with the given position and scale.
     * @param position the positon vector to initialize with
     * @param scale the scale vector to initialize with
     */
    public Transform2D(Vector2f position, Vector2f scale) {
        this();
        this.position.set(position);
        this.scale.set(scale);
    }

    /**
     * Create a new {@link Transform2D} and initialize its components to that of the given transform.
     * @param other the other transform to copy from
     */
    public Transform2D(Transform2D other) {
        this();
        position.set(other.position);
        scale.set(other.scale);
        rotation = other.rotation;
        zIndex = other.zIndex;
        relativeZIndex = other.relativeZIndex;
    }

    /**
     * Create a new {@link Transform2D} and initialize its component to that of this transform.
     * @return a new {@link Transform2D}
     */
    public Transform2D copy() {
        return new Transform2D(this);
    }

    /**
     * Apply the values of the source transform to the destination transform.
     * If either of the source or destination is null, this will do nothing.
     * @param source the transform to get values from
     * @param destination the transform to apply values to
     */
    public static void copy(Transform2D source,Transform2D destination) {
        if (source == null || destination == null) return;
        destination.position.set(source.position);
        destination.scale.set(source.scale);
        destination.rotation = source.rotation;
        destination.zIndex = source.zIndex;
        destination.relativeZIndex = source.relativeZIndex;
    }

    @Override
    public void imgui() {
        EditorTemplate.render(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof Transform2D t)) return false;

        return t.position.equals(this.position) &&
                t.scale.equals(this.scale) &&
                t.rotation == this.rotation &&
                t.zIndex == this.zIndex &&
                t.relativeZIndex == this.relativeZIndex;
    }
}

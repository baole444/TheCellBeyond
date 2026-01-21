package TheCellBeyond;

import components.Component;
import editor.ImEditorGui;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
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
    public Transform2D() {}

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
        this.position.set(position);
        this.scale.set(scale);
    }

    /**
     * Create a new {@link Transform2D} and initialize its components to that of the given transform.
     * @param other the other transform to copy from
     */
    public Transform2D(Transform2D other) {
        position.set(other.position);
        scale.set(other.scale);
        rotation = other.rotation;
        zIndex = other.zIndex;
        relativeZIndex = other.relativeZIndex;
    }

    /**
     * Copy the values of this transform's component to that of the designated transform.
     * @param destination the transform that needs copying
     */
    public void copy(Transform2D destination) {
        this.position.set(destination.position);
        this.scale.set(destination.scale);
        this.rotation = destination.rotation;
        this.zIndex = destination.zIndex;
        this.relativeZIndex = destination.relativeZIndex;
    }

    /**
     * Create a new {@link Transform2D} and initialize its component to that of this transform.
     * @return a new {@link Transform2D}
     */
    public Transform2D copy() {
        return new Transform2D(this);
    }

    @Override
    public void imgui() {
        String compositeID = "Transform2D##" + getUUID();
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean open = ImGui.collapsingHeader(compositeID);
        ImGui.popStyleColor(1);
        if (!open) return;
        ImGui.separator();
        ImEditorGui.dragVec2Ctrl("Position", position, 0.0f, WorldUnit.WorldUnitsPerPixel, this);
        ImEditorGui.dragVec2Ctrl("Scale", scale, 1.0f, this);
        rotation = ImEditorGui.dragFloatCtrl("Rotation", rotation, this);
        zIndex = ImEditorGui.dragIntCtrl("Z-Index", zIndex, this);
        ImBoolean rZIndex = new ImBoolean(relativeZIndex);
        if (ImGui.checkbox("Z-Index as Relative##Relative_Transform_ZIndex_" + getUUID(), rZIndex)) relativeZIndex = rZIndex.get();
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text("relativeZIndex = " + (relativeZIndex ? "true" : "false"));
            ImGui.spacing();
            ImGui.text("If \"true\", the final z-Index of this transform is relative to the parent/owning object.");
            ImGui.text("For example, if this transform's z-Index is 2 and final z-Index of the parent/owning object is 3, this transform's effective z-Index is 2 + 3 = 5");
            ImGui.endTooltip();
        }
        ImGui.separator();
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

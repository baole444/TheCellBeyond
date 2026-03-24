package components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.Transform2D;
import imgui.ImGui;
import org.joml.Vector2f;
import render.commands.RenderCommand;
import render.commands.TransformCommand;

/**
 * Component2D is an abstract 2D component, used as base class for all component types that need spatial world present.
 * <p>
 * Component2D's transformation consists of local and global (effective) transform, similar to {@link GameObject2D}.
 * Its effective transform is a combination of its local transform and optionally,
 * the global transform of the 2D object it is mounted to.
 * @apiNote
 * Mounting a 2D component to a normal {@link TheCellBeyond.GameObject} will make its local transform to become global transform.
 *
 */
public abstract class Component2D extends RenderableComponent {
    /**
     * This 2D component's local transform.
     */
    protected final Transform2D localTransform2D = new Transform2D();

    /**
     * This 2D component's global transform.
     */
    private transient Transform2D effectiveTransform2D = null;

    /**
     * Is this 2D component's global transform needs update.
     */
    private transient boolean isTransformDirty = true;

    /**
     * Create a new {@link Component2D} component.
     */
    public Component2D() {
        String name = Component2D.class.getSimpleName();
        this(name);
    }

    /**
     * Create a new {@link Component2D} component with the given name.
     * @param name the new name for the component
     */
    public Component2D(String name) {
        if (invalidName(name)) name = Component2D.class.getSimpleName();
        super(name);
    }

    @Override
    public int renderZIndex() {
        return globalZIndex();
    }

    @Override
    public void start() {
        super.start();
        setTransformDirty();
    }

    @Override
    public void editorStart() {
        super.editorStart();
        setTransformDirty();
    }

    /**
     * Get the global (effective) transform of this 2D component.
     * @return the global transform
     */
    public Transform2D effectiveTransform() {
        updateEffectiveTransform();
        return effectiveTransform2D;
    }

    /**
     * Get the global position from the owning 2D object of this 2D component.
     * If the owning object of this component is not at least of class {@link GameObject2D},
     * this will return a copy of this component's local position.
     * @return the global positon vector
     */
    public Vector2f objectWorldPosition() {
        if (gameObject instanceof GameObject2D go2D) return go2D.globalPosition();
        return new Vector2f(localTransform2D.position);
    }

    @Override
    public void imgui() {
        super.imgui();
        Transform2D editing = new Transform2D(localTransform2D);
        ImGui.indent();
        localTransform2D.imgui();
        ImGui.unindent();
        if (!editing.equals(localTransform2D)) setTransformDirty();
    }

    /**
     * Get the local position of this 2D component.
     * <p>
     * The local position is optionally relative to the 2D object that this component mounted to.
     * @return the local position vector
     * @see #position(float, float) Set the local position for this 2D component
     * @apiNote
     * Directly modify the vector returned by this method will not trigger the transform dirty flag.
     */
    public Vector2f position() {
        return localTransform2D.position;
    }

    /**
     * Set the local position of this 2D component using the given vector.
     * <p>
     * This will trigger the transform dirty flag.
     * @param position the vector to update the local position with
     */
    public void position(Vector2f position) {
        if (position == null) return;
        position(position.x, position.y);
    }

    /**
     * Set the local position of this 2D component using the given vector.
     * <p>
     * This will trigger the transform dirty flag.
     * @param x the x component of the position vector
     * @param y the y component of the position vector
     */
    public void position(float x, float y) {
        localTransform2D.position.set(x, y);
        setTransformDirty();
    }

    /**
     * Get the global position of this 2D component.
     * <p>
     * The global position is absolute position in world space, a combination of this component's local transform,
     * and the optional global transform that of the 2D object this component mounted to.
     * </p>
     * If the transform dirty flag was triggered for this component, it will update the global transform first.
     * @return the global position vector
     * @see #globalPosition(float, float) Set the global position for this 2D component
     * @apiNote
     * Directly modify the vector returned by this method will not trigger the transform dirty flag.
     */
    public Vector2f globalPosition() {
        return effectiveTransform().position;
    }

    /**
     * Set the global position of this 2D component using the given vector.
     * <p>
     * This will trigger the transform dirty flag.
     * @param globalPosition the vector to update the global position with
     * @apiNote
     * This is an indirect calculation of the local position for this 2D component,
     * useful for when the targeted absolute position is known or easier to obtain.
     */
    public void globalPosition(Vector2f globalPosition) {
        if (globalPosition == null) return;
        globalPosition(globalPosition.x, globalPosition.y);
    }

    /**
     * Set the global position of this 2D component using the given vector.
     * <p>
     * This will trigger the transform dirty flag.
     * @param x the x component of the position vector
     * @param y the y component of the position vector
     * @apiNote
     * This is an indirect calculation of the local position for this 2D component,
     * useful for when the targeted absolute position is known or easier to obtain.
     */
    public void globalPosition(float x, float y) {
        if (!(gameObject instanceof GameObject2D go2D)) localTransform2D.position.set(x, y);
        else {
            Vector2f goPosition = go2D.globalPosition();
            localTransform2D.position.set(x, y).sub(goPosition);
        }
        setTransformDirty();
    }

    /**
     * Get the local rotation of this 2D component.
     * <p>
     * The local rotation is optionally relative to the 2D object that this component mounted to.
     * @return the local rotation angle (in Degree)
     */
    public float rotation() {
        return localTransform2D.rotation;
    }

    /**
     * Set the local rotation of this 2D component using the given angle.
     * <p>
     * This will trigger the transform dirty flag.
     * @param rotationDegrees the rotation angle (in Degree) to update the local rotation with
     */
    public void rotation(float rotationDegrees) {
        localTransform2D.rotation = rotationDegrees;
        setTransformDirty();
    }

    /**
     * Get the global rotation of this 2D component.
     * <p>
     * The global rotation is absolute rotation in world space, a combination of this component's local transform,
     * and the optional global transform that of the 2D object this component mounted to.
     * </p>
     * If the transform dirty flag was triggered for this component, it will update the global transform first.
     * @return the global rotation angle (in Degree)
     */
    public float globalRotation() {
        return effectiveTransform().rotation;
    }

    /**
     * Set the global rotation of this 2D component using the given angle.
     * <p>
     * This will trigger the transform dirty flag.
     * @param globalRotationDegrees the rotation angle (in Degree) to update the global rotation with
     * @apiNote
     * This is an indirect calculation of local rotation for this 2D object,
     * useful for when the targeted final angle is known or easier to obtain.
     */
    public void globalRotation(float globalRotationDegrees) {
        if (gameObject instanceof GameObject2D go2D) {
            float goRotation = go2D.globalRotation();
            localTransform2D.rotation = globalRotationDegrees - goRotation;
        } else {
            localTransform2D.rotation = globalRotationDegrees;
        }
        setTransformDirty();
    }

    /**
     * Get the local scale of this 2D component.
     * <p>
     * The local scale is optionally relative to the 2D object that this component mounted to.
     * @return the local scale vector
     * @see #scale(float, float) Set the local scale for this 2D component
     * @apiNote
     * Directly modify the vector returned by this method will not trigger the transform dirty flag.
     */
    public Vector2f scale() {
        return localTransform2D.scale;
    }

    /**
     * Set the local scale of this 2D component.
     * <p>
     * This will trigger the transform dirty flag.
     * @param scale the vector to update the local scale with
     */
    public void scale(Vector2f scale) {
        if (scale == null) return;
        scale(scale.x, scale.y);
    }

    /**
     * Set the local scale of this 2D component.
     * <p>
     * This will trigger the transform dirty flag.
     * @param x the x component of the scale vector
     * @param y the y component of the scale vector
     */
    public void scale(float x, float y) {
        localTransform2D.scale.set(x, y);
        setTransformDirty();
    }

    /**
     * Get the global scale of this 2D component.
     * <p>
     * The global scale is absolute scale in world space, a combination of this component's local transform,
     * and the optional global transform that of the 2D object this component mounted to.
     * </p>
     * If the transform dirty flag was triggered for this component, it will update the global transform first.
     * @return the global scale vector
     * @apiNote
     * Directly modify the vector returned by this method will not trigger the transform dirty flag.
     */
    public Vector2f globalScale() {
        return effectiveTransform().scale;
    }

    /**
     * Set the global scale of this 2D component using the given vector.
     * <p>
     * This will trigger the transform dirty flag.
     * @param globalScale the vector to update the global scale with
     * @apiNote
     * This is an indirect calculation of local scale for this 2D component,
     * useful for when the targeted effective scale is known for easier to obtain.
     */
    public void globalScale(Vector2f globalScale) {
        if (globalScale == null) return;
        globalScale(globalScale.x, globalScale.y);
    }

    /**
     * Set the global scale of this 2D component using the given vector.
     * <p>
     * This will trigger the transform dirty flag.
     * @param x the x component of the scale vector
     * @param y the y component of the scale vector
     * @apiNote
     * This is an indirect calculation of local scale for this 2D component,
     * useful for when the targeted effective scale is known for easier to obtain.
     */
    public void globalScale(float x, float y) {
        if (!(gameObject instanceof GameObject2D go2D)) localTransform2D.scale.set(x, y);
        else {
            Vector2f goScale = go2D.globalScale();
            if (goScale.x == 0.0f || goScale.y == 0.0f) {
                localTransform2D.scale.set(x, y);
                setTransformDirty();
                return;
            }
            localTransform2D.scale.set(x, y).div(goScale);
        }
        setTransformDirty();
    }

    /**
     * Get the local z-index of this 2D object.
     * <p>
     * The local z-index can be absolute in world space,
     * or optionally relative to the 2D object that this component mounted to.
     * @return the local z-index value
     * @see #isZIndexRelative() Check if z-index is relative or not
     */
    public int zIndex() {
        return localTransform2D.zIndex;
    }

    /**
     * Set the local z-index of this 2D component using the given value.
     * <p>
     * This will trigger the transform dirty flag.
     * @param zIndex the z-index value to update with
     */
    public void zIndex(int zIndex) {
        localTransform2D.zIndex = zIndex;
        setTransformDirty();
    }

    /**
     * Get the global z-index of this 2D component.
     * <p>
     * The global z-index is absolute z-index in world space, a combination of this component's local transform,
     * and the optional global transform that of the 2D object this component mounted to.
     * </p>
     * If this 2D component's local z-index is absolute, it will override the value of global z-index.
     * @return the global z-index value
     */
    public int globalZIndex() {
        return effectiveTransform().zIndex;
    }

    /**
     * Check if the z-index of this 2D component is relative z-index or not.
     * @return true if relative
     * @see Transform2D#relativeZIndex Transform2D relative z-index
     */
    public boolean isZIndexRelative() {
        return localTransform2D.relativeZIndex;
    }

    /**
     * Set the z-index of this 2D component as relative z-index.
     * <p>
     * This will trigger the transform dirty flag.
     * @see Transform2D#relativeZIndex Transform2D relative z-index
     */
    public void setZIndexAsRelative() {
        localTransform2D.relativeZIndex = true;
        setTransformDirty();
    }

    /**
     * Set the z-index of this 2D component as absolute z-index.
     * <p>
     * This will trigger the transform dirty flag.
     * @see Transform2D#relativeZIndex Transform2D relative z-index
     */
    public void setZIndexAsAbsolute() {
        localTransform2D.relativeZIndex = false;
        setTransformDirty();
    }

    /**
     * Trigger the {@link #isTransformDirty} flag of this 2D component.
     * This first call {@link #onTransformDirty()}.
     */
    public void setTransformDirty() {
        onTransformDirty();
        isTransformDirty = true;
    }

    /**
     * Optional hook for additional 2D component's transform dirty logic.
     */
    protected void onTransformDirty() {}

    /**
     * Internal update of this component's global transform.
     */
    private void updateEffectiveTransform() {
        if (!isTransformDirty && effectiveTransform2D != null) return;
        if (effectiveTransform2D == null) effectiveTransform2D = new Transform2D();
        if (!(gameObject instanceof GameObject2D go2D)) {
            Transform2D.copy(localTransform2D, effectiveTransform2D);
            isTransformDirty = false;
            return;
        }
        if (go2D.isTransformUpdating()) return;
        Transform2D goTransform2D = go2D.globalTransform();
        Transform2D.copy(goTransform2D, effectiveTransform2D);
        addTransforms(effectiveTransform2D, localTransform2D);
        isTransformDirty = false;
    }

    /**
     * Addition of 2 transform
     * @param target the transform that will receive the updated value
     * @param offset the source of the update
     */
    private void addTransforms(Transform2D target, Transform2D offset) {
        if (target == null || offset == null) return;
        target.position.add(offset.position);
        target.rotation += offset.rotation;
        target.scale.mul(offset.scale);
        target.relativeZIndex = offset.relativeZIndex;
        if (offset.relativeZIndex) {
            target.zIndex += offset.zIndex;
            return;
        }
        target.zIndex = offset.zIndex;
    }

    @Override
    public TransformCommand buildTransformCommand() {
        if (localTransform2D.isIdentity()) return null;
        TransformCommand command = TransformCommand.acquire();
        Transform2D effectiveTransform = effectiveTransform();
        command.position.set(effectiveTransform.position);
        command.scale.set(effectiveTransform.scale);
        command.rotationDegrees = effectiveTransform.rotation;
        command.zIndex = effectiveTransform.zIndex;
        command.markChanged();
        return command;
    }
}

package TheCellBeyond;

import components.*;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import render.commands.TransformCommand;

import java.util.List;

/**
 * GameObject2D is the base type for all 2D related objects.
 * It inherited {@link GameObject}'s properties with spatial transformation built in.
 * This allows the object to exist in the logic spatial world (game world) with position, rotation and scale.
 * <p>
 * GameObject2D's spatial transformation consist of:
 * <ul>
 *     <li> <b>Local transform</b> is the offset transform relative to the object's <u>nearest ancestor</u>.
 *     For objects that are at scene's root level, this is also their global transform.
 *     </li>
 *     <li> <b>Global transform</b> is the effective transform of each object in the world space.
 *     It is computed using the <u>nearest ancestor</u>'s global transform and the object's local transform.
 *     </li>
 * </ul>
 * GameObject2D utilizes transform dirty flag to passively update its global transform.
 * This flag can propagate down the hierarchy tree from the current object to all its descendants of this type or its subclasses.
 * <p>
 * GameObject2D also responsible for informing its {@link Component2D} and set the component's transform dirty flag.
 * </p>
 * <b>Inherited by:</b> {@link physic2d.PhysicBody2D}
 * @see Transform2D Transform2D data structure
 */
public class GameObject2D extends RenderableObject {
    /**
     * This 2D object's local transform.
     */
    private final Transform2D localTransform2D;
    /**
     * This 2D object's global transform.
     */
    private transient final Transform2D globalTransform2D;
    /**
     * Is this 2D object's global transform needs update.
     */
    private transient boolean isTransformDirty = true;
    /**
     * Is this 2D object's global transform in the process of updating.
     */
    private transient boolean isTransformUpdating = false;
    /**
     * Is this 2D object's in the process of triggering its components' transform dirty flag.
     */
    private transient boolean isNotifyingComponent = false;
    /**
     * Matrix use for calculating local transform.
     */
    private transient final Matrix3x2f localMatrix = new Matrix3x2f();
    /**
     * Matrix use for calculating global transform.
     */
    private transient final Matrix3x2f globalMatrix = new Matrix3x2f();
    /**
     * Temporary matrix for coordinate conversion.
     */
    private transient final Matrix3x2f tmpMatrix = new Matrix3x2f();
    /**
     * Previous global transform used for interpolation with physic frame and rendering.
     * This transform is only up-to-date if {@link #hasPreviousTransform} is {@code true}.
     */
    public final transient Transform2D previousTransform2D = new Transform2D();
    /**
     * Flag used to indicate that {@link #previousTransform2D} had been updated.
     */
    protected transient boolean hasPreviousTransform = false;

    /**
     * Create a new {@link GameObject2D}.
     */
    public GameObject2D() {
        String name = GameObject2D.class.getSimpleName();
        this(name);
    }

    /**
     * Create a new {@link GameObject2D} with the given name.
     * @param name the new name for the 2D object
     */
    public GameObject2D(String name) {
        if (invalidName(name)) name = GameObject2D.class.getSimpleName();
        super(name);
        localTransform2D = new Transform2D();
        globalTransform2D = new Transform2D();
    }

    /**
     * Get the local position of this 2D object.
     * <p>
     * The local position is relative to this object's nearest ancestor of {@link GameObject2D} type or its subclasses.
     * @return the local position vector
     * @see #position(float, float) Set the local position for this 2D object
     * @apiNote
     * Directly modify the vector returned by this method will not trigger the transform dirty flag.
     */
    public Vector2f position() {
        return localTransform2D.position;
    }

    /**
     * Set the local position of this 2D object using the given vector.
     * <p>
     * This will trigger the transform dirty flag.
     * @param position the vector to update the local position with
     */
    public void position(Vector2f position) {
        if (position == null) return;
        localTransform2D.position.set(position);
        setTransformDirty();
    }

    /**
     * Set the local position of this 2D object using the given values.
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
     * Get the global position of this 2D object.
     * <p>
     * The global position is absolute position in world space, a combination of this object's local transform
     * and the global transform of its nearest ancestor of {@link GameObject2D} type or its subclasses.
     * </p>
     * If the transform dirty flag was triggered for this object, it will update the global transform first.
     * @return the global position vector
     * @see #globalPosition(float, float) Set the global position for this 2D object
     * @apiNote
     * Directly modify the vector returned by this method will not trigger the transform dirty flag.
     */
    public Vector2f globalPosition() {
        updateGlobalTransform();
        return globalTransform2D.position;
    }

    /**
     * Set the global position of this 2D object using the given vector.
     * <p>
     * This will trigger the transform dirty flag.
     * @param globalPosition the vector to update the global position with
     * @apiNote
     * This is an indirect calculation of local position for this 2D object,
     * useful for when the targeted absolute position is known or easier to obtain.
     */
    public void globalPosition(Vector2f globalPosition) {
        if (globalPosition == null) return;
        globalPosition(globalPosition.x, globalPosition.y);
    }

    /**
     * Set the global position of this 2D object using the given values.
     * <p>
     * This will trigger the transform dirty flag.
     * @param x the x component of the position vector
     * @param y the y component of the position vector
     * @apiNote
     * This is an indirect calculation of local position for this 2D object,
     * useful for when the targeted absolute position is known or easier to obtain.
     */
    public void globalPosition(float x, float y) {
        GameObject2D parent2D = getParent2D();
        if (parent2D == null) localTransform2D.position.set(x, y);
        else {
            parent2D.updateGlobalTransform();
            parent2D.tmpMatrix.set(parent2D.globalMatrix).invert();
            parent2D.tmpMatrix.transformPosition(x, y, localTransform2D.position);
        }
        setTransformDirty();
    }

    /**
     * Get the local rotation of this 2D object.
     * <p>
     * The local rotation is relative to this object's nearest ancestor of {@link GameObject2D} type or its subclasses.
     * @return the local rotation angle (in Degrees)
     */
    public float rotation() {
        return localTransform2D.rotation;
    }

    /**
     * Set the local rotation of this 2D object using the given angle.
     * <p>
     * This will trigger the transform dirty flag.
     * @param rotationDegrees the rotation angle (in Degrees) to update the local rotation with
     */
    public void rotation(float rotationDegrees) {
        localTransform2D.rotation = rotationDegrees;
        setTransformDirty();
    }

    /**
     * Get the global rotation of this 2D object.
     * <p>
     * The global rotation is absolute rotation in world space, a combination of this object's local transform
     * and the global transform of its nearest ancestor of {@link GameObject2D} type or its subclasses.
     * </p>
     * If the transform dirty flag was triggered for this object, it will update the global transform first.
     * @return the global rotation angle (in Degree)
     */
    public float globalRotation() {
        updateGlobalTransform();
        return globalTransform2D.rotation;
    }

    /**
     * Set the global rotation of this 2D object using the given angle.
     * <p>
     * This will trigger the transform dirty flag.
     * @param globalRotationDegrees the rotation angle (in Degrees) to update the global rotation with
     * @apiNote
     * This is an indirect calculation of local rotation for this 2D object,
     * useful for when the targeted final angle is known or easier to obtain.
     */
    public void globalRotation(float globalRotationDegrees) {
        GameObject2D parent2D = getParent2D();
        if (parent2D == null) localTransform2D.rotation = globalRotationDegrees;
        else localTransform2D.rotation = globalRotationDegrees - parent2D.globalRotation();
        setTransformDirty();
    }

    /**
     * Get the local scale of this 2D object.
     * <p>
     * The local scale is relative to this object's nearest ancestor of {@link GameObject2D} type or its subclasses.
     * @return the local scale vector
     * @see #scale(float, float) Set the local scale for this 2D object
     * @apiNote
     * Directly modify the vector returned by this method will not trigger the transform dirty flag.
     */
    public Vector2f scale() {
        return localTransform2D.scale;
    }

    /**
     * Set the local scale of this 2D object.
     * <p>
     * This will trigger the transform dirty flag.
     * @param scale the vector to update the local scale with
     */
    public void scale(Vector2f scale) {
        if (scale == null) return;
        localTransform2D.scale.set(scale);
        setTransformDirty();
    }

    /**
     * Set the local scale of this 2D object.
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
     * Get the global scale of this 2D object.
     * <p>
     * The global scale is absolute scale in world space, a combination of this object's local transform
     * and the global transform of its nearest ancestor of {@link GameObject2D} type or its subclasses.
     * </p>
     * If the transform dirty flag was triggered for this object, it will update the global transform first.
     * @return the global scale vector
     * @see #globalScale(float, float) Set the global scale for this 2D object
     * @apiNote
     * Directly modify the vector returned by this method will not trigger the transform dirty flag.
     */
    public Vector2f globalScale() {
        updateGlobalTransform();
        return globalTransform2D.scale;
    }

    /**
     * Set the global scale of this 2D object using the given vector.
     * <p>
     * This will trigger the transform dirty flag.
     * @param globalScale the vector to update the global scale with
     * @apiNote
     * This is an indirect calculation of local scale for this 2D object,
     * useful for when the targeted effective scale is known or easier to obtain.
     */
    public void globalScale(Vector2f globalScale) {
        if (globalScale == null) return;
        globalScale(globalScale.x, globalScale.y);
    }

    /**
     * Set the global scale of this 2D object using the given vector.
     * <p>
     * This will trigger the transform dirty flag.
     * @param x the x component of the scale vector
     * @param y the y component of the scale vector
     * @apiNote
     * This is an indirect calculation of local scale for this 2D object,
     * useful for when the targeted effective scale is known or easier to obtain.
     */
    public void globalScale(float x, float y) {
        GameObject2D parent2D = getParent2D();
        if (parent2D == null) localTransform2D.scale.set(x, y);
        else {
            Vector2f parentScale = parent2D.globalScale();
            if (parentScale.x == 0.0f || parentScale.y == 0.0f) {
                localTransform2D.scale.set(x, y);
                setTransformDirty();
                return;
            }
            localTransform2D.scale.set(x, y).div(parentScale);
        }
        setTransformDirty();
    }

    /**
     * Get the local z-index of this 2D object.
     * <p>
     * The local z-index can be absolute in world space or relative to this object's nearest ancestor of {@link GameObject2D} type or its subclasses.
     * @return the local z-index value
     * @see #isZIndexRelative() Check if z-index is relative or not
     */
    public int zIndex() {
        return localTransform2D.zIndex;
    }

    /**
     * Set the local z-index of this 2D object using the given value.
     * <p>
     * This will trigger the transform dirty flag.
     * @param zIndex the z-index value to update with
     */
    public void zIndex(int zIndex) {
        localTransform2D.zIndex = zIndex;
        setTransformDirty();
    }

    /**
     * Get the global z-index of this 2D object.
     * <p>
     * The global z-index is absolute z-index in world space, a combination of this object's local transform
     * and the global transform of its nearest ancestor of {@link GameObject2D} type or its subclasses.
     * </p>
     * If this 2D object's local z-index is absolute, it will override the value of global z-index.
     * @return the global z-index value
     */
    public int globalZIndex() {
        updateGlobalTransform();
        return globalTransform2D.zIndex;
    }

    /**
     * Check if the z-index of this 2D object is relative z-index or not.
     * @return true if relative
     * @see Transform2D#relativeZIndex Transform2D relative z-index
     */
    public boolean isZIndexRelative() {
        return localTransform2D.relativeZIndex;
    }

    /**
     * Set the z-index of this 2D object as relative z-index.
     * <p>
     * This will trigger the transform dirty flag.
     * @see Transform2D#relativeZIndex Transform2D relative z-index
     */
    public void setZIndexAsRelative() {
        localTransform2D.relativeZIndex = true;
        setTransformDirty();
    }

    /**
     * Set the z-index of this 2D object as absolute z-index.
     * <p>
     * This will trigger the transform dirty flag.
     * @see Transform2D#relativeZIndex Transform2D relative z-index
     */
    public void setZIndexAsAbsolute() {
        localTransform2D.relativeZIndex = false;
        setTransformDirty();
    }

    /**
     * Get the nearest ancestor of type {@link GameObject2D} or its subclasses.
     * <p>
     * For example, consider this hierarchy tree:
     * {@snippet lang="TEXT":
     *    A (GameObject2D)
     *    |_B (GameObject2D)
     *      |_C (GameObject)
     *        |_This object
     * }
     * <br>
     * This method will return 2D object {@code B}, object {@code C} was skipped
     * since it is not the correct {@link GameObject2D} type or its subclasses.
     * @return the nearest ancestor of type {@link GameObject2D} or its subclasses
     */
    public GameObject2D getParent2D() {
        GameObject parent = getParent();
        while (parent != null) {
            if (parent instanceof GameObject2D parent2D) return parent2D;
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Move this 2D object by the given offset.
     * <p>
     * The offset is added to local position. This will trigger the transform dirty flag.
     * </p>
     * For example, move a 2D object to the right by 0.16 and down by 0.32:
     * {@snippet lang = "java":
     * public class Move() {
     *     public static void run() {
     *         GameObject2D object2D = new GameObject2D();
     *         object2D.position(1.0f, 1.0f); // local position set to (1.0, 1.0)
     *
     *         object2D.translate(new Vector2f(0.16f, -0.32f));
     *         Vector2f pos = object2D.position(); // local position is (1.16, 0.68)
     *     }
     * }
     * }
     * @param offset the vector to move by
     * @see #position(Vector2f) Set the local position
     */
    public void translate(Vector2f offset) {
        localTransform2D.position.add(offset);
        setTransformDirty();
    }

    /**
     * Rotate this 2D object by the given angle.
     * <p>
     * The angle is added to local rotation. This will trigger the transform dirty flag.
     * </p>
     * For example, rotate a 2D object by 30 degrees clockwise:
     * {@snippet lang = "java":
     * public class Rotate() {
     *     public static void run() {
     *         GameObject2D object2D = new GameObject2D();
     *         object2D.rotation(60.0f); // local rotation set to 60.0 degrees
     *
     *         object2D.rotate(30.0f);
     *         float rotation = object2D.rotation(); // local rotation is 90 degrees
     *     }
     * }
     * }
     * @param angle the angle value (in Degrees) to rotate by
     * @see #rotation(float) Set the local rotation
     */
    public void rotate(float angle) {
        localTransform2D.rotation += angle;
        setTransformDirty();
    }

    /**
     * Scale this 2D object by the given factor.
     * <p>
     * The local scale is multiplied by this factor. This will trigger the transform dirty flag.
     * </p>
     * For example, reduce the scale of a 2D object by half:
     * {@snippet lang = "java":
     * public class Scale() {
     *     public static void run() {
     *         GameObject2D object2D = new GameObject2D();
     *         object2D.scale(3.0f, 2.0f); // local scale set to (3.0, 2.0)
     *
     *         object2D.scaleBy(new Vector2f(0.5f));
     *         Vector2f scale = object2D.scale(); // local scale is (1.5, 1)
     *     }
     * }
     * }
     * @param factor the factor vector to multiply by
     * @see #scale(Vector2f) Set the local scale
     */
    public void scaleBy(Vector2f factor) {
        localTransform2D.scale.mul(factor);
        setTransformDirty();
    }

    /**
     * Convert a position from local to world space.
     * <p>
     * This applies the global transform of this 2D object to the given local position,
     * result in the equivalent position in world space.
     * </p>
     * For example:
     * {@snippet lang = "java":
     * public class ConvertGlobal() {
     *     public static void run() {
     *         GameObject2D object2D = new GameObject2D();
     *         object2D.position(2.0f, 4.0f);
     *         object2D.rotation(30.0f);
     *
     *         Vector2f localPos = new Vector2f(3.0f);
     *         Vector2f worldPos = object2D.toGlobal(localPos);
     *     }
     * }
     * }
     * The {@code worldPos} vector's values is the result of {@code localPos} rotated and translated by the {@code object2D}'s transform.
     * @param localPosition the position vector to be converted with
     * @return a new {@link Vector2f} that is the position in world space
     * @see #toLocal(Vector2f) Convert from global to local position
     */
    public Vector2f toGlobal(Vector2f localPosition) {
        if (localPosition == null) return new Vector2f();
        updateGlobalTransform();
        Vector2f result = new Vector2f();
        globalMatrix.transformPosition(localPosition, result);
        return result;
    }

    /**
     * Convert a position from world to local space.
     * <p>
     * This applies the inverse of this 2D object's global transform to the given global position,
     * result in the equivalent position in the local space.
     * </p>
     * For example:
     * {@snippet lang = "java":
     * public class ConvertLocal() {
     *     public static void run() {
     *         GameObject2D object2D = new GameObject2D();
     *         object2D.position(3.0f, 1.0f);
     *         object2D.rotation(60.0f);
     *
     *         Vector2f worldPos = new Vector2f(1.0f, 2.0f);
     *         Vector2f localPos = object2D.toLocal(worldPos);
     *     }
     * }
     * }
     * The {@code localPos} vector's values is the result of {@code worldPos} transformed into {@code object2D}'s local space.
     * @param globalPosition the position vector to be converted with
     * @return a new {@link Vector2f} that is the position in this object's local space
     * @see #toGlobal(Vector2f) Convert from local to global position
     */
    public Vector2f toLocal(Vector2f globalPosition) {
        if (globalPosition == null) return new Vector2f();
        updateGlobalTransform();
        tmpMatrix.set(globalMatrix).invert();
        Vector2f result = new Vector2f();
        tmpMatrix.transformPosition(globalPosition, result);
        return result;
    }

    /**
     * Update global transform and consume the transform dirty flag.
     * <p>
     * Matrix order: L = Translate * Rotate * Scale.
     * This is read from right to left, result in SRT transformation.
     * @see <a href="https://gamedev.stackexchange.com/questions/29260/transform-matrix-multiplication-order">Order explanation</a>
     */
    private void updateGlobalTransform() {
        if (!isTransformDirty || isTransformUpdating) return;
        isTransformUpdating = true;
        try {
            GameObject2D parent2D = getParent2D();
            updateGlobalPosition(parent2D);
            globalTransform2D.rotation = (float) Math.toDegrees(Math.atan2(globalMatrix.m01(), globalMatrix.m00()));
            updateGlobalScale();
            updateGlobalZIndex(parent2D);
            isTransformDirty = false;
            updateComponent2Ds();
        } finally {
            isTransformUpdating = false;
        }
    }

    /**
     * Internal update of global transform's ZIndex.
     * @param parent2D the parent 2D object
     * @see #updateGlobalTransform() internal update of global transform
     */
    private void updateGlobalZIndex(GameObject2D parent2D) {
        globalTransform2D.relativeZIndex = localTransform2D.relativeZIndex;
        if (localTransform2D.relativeZIndex && parent2D != null) {
            globalTransform2D.zIndex = parent2D.globalTransform2D.zIndex + localTransform2D.zIndex;
            return;
        }
        globalTransform2D.zIndex = localTransform2D.zIndex;
    }

    /**
     * Internal update of global transform's position.
     * @param parent2D the parent 2D object
     * @see #updateGlobalTransform() internal update of global transform
     */
    private void updateGlobalPosition(GameObject2D parent2D) {
        localMatrix.identity()
                .translate(localTransform2D.position)
                .rotate((float) Math.toRadians(localTransform2D.rotation))
                .scale(localTransform2D.scale);
        if (parent2D != null) {
            parent2D.updateGlobalTransform();
            globalMatrix.set(parent2D.globalMatrix).mul(localMatrix);
        } else {
            globalMatrix.set(localMatrix);
        }
        Vector2f translation = new Vector2f(globalMatrix.m20(), globalMatrix.m21());
        globalTransform2D.position.set(translation);
    }

    /**
     * Internal update of global transform's scale.
     * @see #updateGlobalTransform() internal update of global transform
     * @apiNote
     * Many thanks to "Whiteaxe" for finding the bug here: "globalMatrix.m10() * globalMatrix.m11()" <- supposed to be "m10()"
     */
    private void updateGlobalScale() {
        float m00Sqr = globalMatrix.m00() * globalMatrix.m00();
        float m01Sqr = globalMatrix.m01() * globalMatrix.m01();
        float m10Sqr = globalMatrix.m10() * globalMatrix.m10();
        float m11Sqr = globalMatrix.m11() * globalMatrix.m11();
        float scaleX = (float) Math.sqrt(m00Sqr + m01Sqr);
        float scaleY = (float) Math.sqrt(m10Sqr + m11Sqr);
        globalTransform2D.scale.set(scaleX, scaleY);
    }

    /**
     * Inform spatial components of this object that their effective transform is outdated.
     */
    private void updateComponent2Ds() {
        if (isNotifyingComponent) return;
        isNotifyingComponent = true;
        try {
            List<Component2D> components = getComponents(Component2D.class);
            components.forEach(Component2D::setTransformDirty);
        } finally {
            isNotifyingComponent = false;
        }
    }

    /**
     * Get a copy for the global matrix of this 2D object.
     * @return a new {@link Matrix3x2f}
     */
    public Matrix3x2f globalMatrix() {
        updateGlobalTransform();
        return new Matrix3x2f(globalMatrix);
    }

    /**
     * Get the local transform of this 2D object.
     * @return the local transform
     * @apiNote
     * Directly modify the values returned by this method will not trigger the transform dirty flag.
     */
    public Transform2D localTransform() {
        return localTransform2D;
    }

    /**
     * Set the values of this 2D object's local transform using the given transform.
     * <p>
     * This will trigger the transform dirty flag if the values are different.
     * @param newTransform the transform to copy values from
     */
    public void localTransform(Transform2D newTransform) {
        if (localTransform2D.equals(newTransform)) return;
        Transform2D.copy(newTransform, localTransform2D);
        setTransformDirty();
    }

    /**
     * Get the global transform of this 2D object.
     * @return a new {@link Transform2D}
     */
    public Transform2D globalTransform() {
        updateGlobalTransform();
        return globalTransform2D;
    }

    /**
     * Trigger the {@link #isTransformDirty} flag of this 2D object.
     * This first call {@link #onTransformDirty()}, then the flag is
     * propagated down to all descendants of type {@link GameObject2D} or its subclasses.
     */
    private void setTransformDirty() {
        onTransformDirty();
        if (!isTransformDirty) {
            isTransformDirty = true;
            if (!isNotifyingComponent) updateComponent2Ds();
        }
        for (GameObject child : getChildren()) {
            if (!(child instanceof GameObject2D child2D)) continue;
            child2D.setTransformDirty();
        }
    }

    /**
     * Optional hook for additional 2D object's transform dirty logic.
     */
    protected void onTransformDirty() {
        renderDirty = true;
    }

    /**
     * Check if this 2D object is in the process of updating its global transform.
     * <p>
     * All values obtained from global transform might be outdated while this flag is true.
     * @return true if currently updating
     */
    public boolean isTransformUpdating() {
        return isTransformUpdating;
    }

    /**
     * Add an object as a child object to this game object. The child object's parent is set to this.
     * <p>
     * The child object cannot be this object itself or its ancestor.
     * For example, consider this hierarchy tree:
     * {@snippet lang="TEXT":
     *    A
     *    |_B
     *      |_C
     *        |_D
     * }
     * <br>
     * Object {@code D} cannot add object {@code A} as its parent.
     * </p>
     * <p>
     * If the child object is of type {@link GameObject2D} or its subclasses,
     * its transform dirty flag will be triggered.
     * @param child the child object to add
     */
    @Override
    public void addChild(GameObject child) {
        super.addChild(child);
        if (child instanceof GameObject2D child2D) child2D.setTransformDirty();
    }

    /**
     * Remove an object as a child object from this game object. The child object's parent is set to {@code null}.
     * <p>
     * If the child object is of type {@link GameObject2D} or its subclasses,
     * its transform dirty flag will be triggered.
     * @param child the child object to be removed
     */
    @Override
    public void removeChild(GameObject child) {
        super.removeChild(child);
        if (child instanceof GameObject2D child2D) child2D.setTransformDirty();
    }

    /**
     * Create a new {@link GameObject2D} from this game object and its components without the hierarchy.
     * <p>
     * The copy process use serialization, ensure all subclasses of {@link GameObject} are supported.
     * </p>
     * The newly created object is a root object.
     * @return a new {@link GameObject2D}
     */
    @Override
    public GameObject2D copy() {
        return copy(false);
    }

    /**
     * Create a new {@link GameObject2D} from this game object,
     * its components and optionally copy all the descendant objects.
     * <p>
     * The copy process use serialization, ensure all subclasses of {@link GameObject} are supported.
     * </p>
     * The newly created object is a root object.
     * @param copyHierarchy option to copy the hierarchy branch from this object
     * @return a new {@link GameObject2D}
     */
    @Override
    public GameObject2D copy(boolean copyHierarchy) {
        GameObject2D copy = (GameObject2D) copySingleObject();
        if (copyHierarchy && !getChildren().isEmpty()) copyDescendants(this, copy);
        return copy;
    }

    @Override
    public int renderZIndex() {
        return globalZIndex();
    }

    @Override
    public TransformCommand buildTransformCommand() {
        TransformCommand command = TransformCommand.acquire();
        Transform2D globalTransform = globalTransform();
        command.submitterID = getUID();
        command.position.set(globalTransform.position);
        command.rotationDegrees = globalTransform.rotation;
        command.scale.set(globalTransform.scale);
        command.zIndex = globalTransform.zIndex;
        command.visible = visible;
        command.modulate.set(selfModulate);
        command.markChanged();
        return command;
    }

    @Override
    public TransformCommand previousTransformCommand() {
        if (!hasPreviousTransform) return null;
        TransformCommand command = TransformCommand.acquire();
        command.submitterID = getUID();
        command.position.set(previousTransform2D.position);
        command.rotationDegrees = previousTransform2D.rotation;
        command.scale.set(previousTransform2D.scale);
        command.zIndex = previousTransform2D.zIndex;
        command.visible = visible;
        command.modulate.set(selfModulate);
        command.markChanged();
        return command;
    }
}

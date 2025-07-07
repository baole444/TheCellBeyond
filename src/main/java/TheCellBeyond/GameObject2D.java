package TheCellBeyond;

import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public class GameObject2D extends GameObject {
    private final Transform localTransform;

    private final Transform globalTransform;

    private transient boolean isTransformDirty = true;

    private transient final Matrix3x2f localMatrix = new Matrix3x2f();
    private transient final Matrix3x2f globalMatrix = new Matrix3x2f();
    private transient final Matrix3x2f tmpMatrix = new Matrix3x2f();

    public GameObject2D(String name) {
        super(name);
        this.localTransform = new Transform();
        this.globalTransform = new Transform();

        // TODO: When GameObject remove its transform, we don't need to do this
        removeComponent(Transform.class);
    }

    public Vector2f getPosition() {
        return localTransform.position;
    }

    public Vector2f getGlobalPosition() {
        updateGlobalTransform();
        return globalTransform.position;
    }

    public void setPosition(Vector2f position) {
        localTransform.position.set(position);
        setDirty();
    }

    public void setPosition(float x, float y) {
        localTransform.position.set(x, y);
        setDirty();
    }

    public void setGlobalPosition(Vector2f globalPosition) {
        GameObject2D parent2D = getParent2D();

        if (parent2D == null) localTransform.position.set(globalPosition);
        else {
            parent2D.updateGlobalTransform();
            parent2D.tmpMatrix.set(parent2D.globalMatrix).invert();
            parent2D.tmpMatrix.transformPosition(globalPosition, localTransform.position);
        }

        setDirty();
    }

    public float getRotation() {
        return localTransform.rotation;
    }

    public float getGlobalRotation() {
        updateGlobalTransform();
        return globalTransform.rotation;
    }

    public void setRotation(float rotation) {
        localTransform.rotation = rotation;
        setDirty();
    }

    public Vector2f getScale() {
        return localTransform.scale;
    }

    public Vector2f getGlobalScale() {
        updateGlobalTransform();
        return globalTransform.scale;
    }

    public void setScale(Vector2f scale) {
        localTransform.scale.set(scale);
        setDirty();
    }

    public GameObject2D getParent2D() {
        GameObject parent = getParent();

        // Recursive search until finding supported parent
        while (parent != null) {
            if (parent instanceof GameObject2D parent2D) return parent2D;

            parent = parent.getParent();
        }

        return null;
    }

    public void translate(Vector2f offset) {
        localTransform.position.add(offset);
        setDirty();
    }

    public void rotate(float angle) {
        localTransform.rotation += angle;
        setDirty();
    }

    public void scale(Vector2f factor) {
        localTransform.scale.mul(factor);
        setDirty();
    }

    public Vector2f toGlobal(Vector2f localPosition) {
        updateGlobalTransform();

        Vector2f result = new Vector2f();
        globalMatrix.transformPosition(localPosition, result);

        return result;
    }

    public Vector2f toLocal(Vector2f globalPosition) {
        updateGlobalTransform();

        tmpMatrix.set(globalMatrix).invert();
        Vector2f result = new Vector2f();
        tmpMatrix.transformPosition(globalPosition, result);

        return result;
    }

    private void updateGlobalTransform() {
        if (!isTransformDirty) return;

        localMatrix.identity()
                .scale(localTransform.scale)
                .rotate((float) Math.toRadians(localTransform.rotation))
                .translate(localTransform.position);

        GameObject2D parent2D = getParent2D();
        if (parent2D != null) {
            parent2D.updateGlobalTransform();
            globalMatrix.set(parent2D.globalMatrix).mul(localMatrix);
        } else {
            globalMatrix.set(localMatrix);
        }

        Vector2f translation = new Vector2f(globalMatrix.m20(), globalMatrix.m21());
        globalTransform.position.set(translation);

        globalTransform.rotation = (float) Math.toDegrees(Math.atan2(globalMatrix.m01(), globalMatrix.m00()));

        float scaleX = (float) Math.sqrt(globalMatrix.m00() * globalMatrix.m00()
                + globalMatrix.m01() * globalMatrix.m01());
        float scaleY = (float) Math.sqrt(globalMatrix.m10() * globalMatrix.m11()
                + globalMatrix.m11() * globalMatrix.m11());

        globalTransform.scale.set(scaleX, scaleY);

        isTransformDirty = false;
    }

    public Matrix3x2f getLocalMatrix() {
        updateGlobalTransform();
        return new Matrix3x2f(globalMatrix);
    }
    
    public Transform getTransForm() {
        return new Transform(localTransform);
    }

    private void setDirty() {
        if (!isTransformDirty) isTransformDirty = true;

        for (GameObject child : getChildren()) {
            if (child instanceof GameObject2D child2D) {
                child2D.setDirty();
            }
        }
    }
}
